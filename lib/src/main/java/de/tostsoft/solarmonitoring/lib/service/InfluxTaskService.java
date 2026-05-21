package de.tostsoft.solarmonitoring.lib.service;

import com.influxdb.exceptions.RequestTimeoutException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.TotalValues;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxFields;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class InfluxTaskService {

  private static final Logger LOG = LoggerFactory.getLogger(InfluxTaskService.class);

  private enum PriceVariable{
      INPUT("price"),
      OUTPUT("priceFeedIn");

      private final String name;

      PriceVariable(String s) {
          name = s;
      }

      public String toString() {
          return this.name;
      }
  }

  @Autowired
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private InfluxConnection influxConnection;

  private final double WsToKwhFactor = 0.000277778 / 1000;

  DecimalFormat decimalFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.US));

  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
  DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  // Pregenerated default query for systems with no totalFilter
  private String defaultTotalQuery;

  @PostConstruct
  private void init(){
    decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

    // Pregenerate default total query once at initialization
    defaultTotalQuery = generateTotalQuery(Collections.emptySet());
  }

  private String generateTotalQuery(Set<String> blacklist){
      if(blacklist == null) {
          blacklist = Collections.emptySet();
      }

      // Expand blacklist to include Price2 variants
      Set<String> expandedBlacklist = new HashSet<>(blacklist);
      for(String field : blacklist) {
          if(StringUtils.endsWith(field, "Price")) {
              // When "SomeFieldPrice" is blacklisted, also blacklist "SomeFieldPrice2"
              expandedBlacklist.add(field + "2");
          }
      }

      ArrayList<String> tripels = new ArrayList<>();
      tripels.add("ProducedKWH");
      tripels.add("ConsumedKWH");
      tripels.add("GridConsumedKWH");
      tripels.add("GridFeedInKWH");
      tripels.add("ProducedKWHPrice");
      tripels.add("ConsumedKWHPrice");
      tripels.add("GridConsumedKWHPrice");
      tripels.add("GridFeedInKWHPrice");
      tripels.add("GridFeedInKWHPrice2");

      StringBuilder query = new StringBuilder();

      query.append("  |> filter(fn: (r) => \n");

      boolean firstCondition = true;
      for(int i = 0;i < tripels.size();i++){
          String field = tripels.get(i);

          // Check each variant and only add if not blacklisted
          if(!expandedBlacklist.contains(field)) {
              if(!firstCondition) query.append(" or\n");
              query.append("     r[\"_field\"] == \"").append(field).append("\"");
              firstCondition = false;
          }

          if(!expandedBlacklist.contains("Calc" + field)) {
              if(!firstCondition) query.append(" or\n");
              query.append("     r[\"_field\"] == \"Calc").append(field).append("\"");
              firstCondition = false;
          }

          if(!expandedBlacklist.contains("CalcByDevices" + field)) {
              if(!firstCondition) query.append(" or\n");
              query.append("     r[\"_field\"] == \"CalcByDevices").append(field).append("\"");
              firstCondition = false;
          }
      }
      query.append("\n)\n  |> drop(columns: [\"type\"])\n");
      query.append("  |> pivot(\n" + "    rowKey: [\"_time\"],\n" + "    columnKey: [\"_field\"],\n" + "    valueColumn: \"_value\"\n" + "  )\n" + "  |> map(fn: (r) => ({\n" + "      r with\n");

      for(int i = 0;i < tripels.size();i++){
          String field = tripels.get(i);
          query.append("      effective").append(field).append(": if exists r.").append(field).append(" then r.").append(field).append(" else if exists r.CalcByDevices").append(field).append(" then r.CalcByDevices").append(field).append(" else if exists r.Calc").append(field).append(" then r.Calc").append(field).append(" else 0.0").append(i + 1 < tripels.size() ? "," : "").append("\n");
      }

      query.append("}))\n" + "  |> reduce(\n" + "    identity: {");

      for(int i = 0;i < tripels.size();i++){
          String field = tripels.get(i);
          query.append("Total").append(field).append(" : 0.0").append(i + 1 < tripels.size() ? "," : "");
      }

      query.append("},\n" + "    fn: (r, accumulator) => ({\n");

      for(int i = 0;i < tripels.size();i++){
          String field = tripels.get(i);
          query.append("      Total").append(field).append(": accumulator.Total").append(field).append(" + r.effective").append(field).append(i + 1 < tripels.size() ? "," : "").append("\n");
      }

      query.append("})\n" + "  )");

      return query.toString();
  }

  private String generateSumQuery(String systemId, InfluxMeasurement influxMeasurement, String bucket, String sourceMeasurement,
                                  String targetMeasurement, String start, String end, double multiplier, Set<PriceVariable> priceVariables){
    return generateSumQuery(systemId, influxMeasurement, bucket, sourceMeasurement,
                            targetMeasurement, start, end, multiplier, priceVariables, null);
  }

  private String generateSumQuery(String systemId, InfluxMeasurement sourceMeasurement,
                                    String bucket, String sourceField, String targetField, String start, String end,
                                    double multiplier, Set<PriceVariable> priceVariables, String valueFilter){
    String multString = decimalFormat.format(multiplier);
    InfluxMeasurement targetInfluxMeasurement = InfluxMeasurement.SOLAR_DAY_DATA;
    String priceMeasurement = targetField+"Price";
    String tmpVarName = "sum_" + sourceField + "_" + targetField;

      StringBuilder q = new StringBuilder(1024);
      q.append(tmpVarName).append(" = from(bucket: \"").append(bucket).append("\")\n")
      .append("  |> range(start: ").append(start).append(", stop: ").append(end).append(")\n")
      .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(sourceMeasurement).append("\")\n")
      .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
      .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(sourceField).append("\" or r[\"_field\"] == \"Duration\")\n")
      .append("  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
      if (valueFilter != null && !valueFilter.isEmpty()) {
          q.append("  |> filter(fn: (r) => r.").append(sourceField).append(" ").append(valueFilter).append(")\n");
      }
      q.append("  |> map(fn: (r) => ({r with _value: r.").append(sourceField).append(" * ").append(multString).append(" * r.Duration}))\n")
      .append("  |> cumulativeSum()\n")
      .append("  |> max()\n")
      .append("  |> map(fn: (r) => ({r with _value: r._value, _time: ").append(start).append(",_measurement: \"").append(targetInfluxMeasurement).append("\",_field:\"").append(targetField).append("\"}))\n")
      .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");

      if(priceVariables != null && priceVariables.contains(PriceVariable.INPUT)) {
          q.append(tmpVarName).append("\n")
          .append("   |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.INPUT).append(", _time: ").append(start).append(",_measurement: \"").append(targetInfluxMeasurement).append("\",_field:\"").append(priceMeasurement).append("\"}))\n ")
          .append("   |> to(bucket: \"").append(bucket).append("\")\n\n");
      }

      if(priceVariables != null && priceVariables.contains(PriceVariable.OUTPUT)) {
          q.append(tmpVarName).append("\n")
                  .append("   |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.OUTPUT).append(", _time: ").append(start).append(",_measurement: \"").append(targetInfluxMeasurement).append("\",_field:\"").append(priceMeasurement).append("2\"}))\n ")
                  .append("   |> to(bucket: \"").append(bucket).append("\")\n\n");
      }

      return q.toString();
  }

  private String generateTotalSumQuery(String systemId,InfluxMeasurement sourceMeasurement,InfluxMeasurement targetMeasurement,
                                       String bucket,String sourceField,String targetField,String start,String end,Set<PriceVariable> priceVariables){

    String priceMeasurement = targetField+"Price";
    String tmpVarName = "total_sum_" + sourceField + "_" + targetField;

    StringBuilder q = new StringBuilder(1024);
    q.append(tmpVarName).append("= from(bucket: \"").append(bucket).append("\")\n")
      .append("  |> range(start: ").append(start).append(", stop: ").append(end).append(")\n")
      .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(sourceMeasurement).append("\")\n")
      .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
      .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(sourceField).append("\")\n")
      .append("  |> filter(fn: (r) => r[\"_value\"] > 0)\n")
      .append("  |> spread() ")
      .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(targetMeasurement).append("\",_field:\"").append(targetField).append("\"}))\n")
      .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");

    if(priceVariables != null && priceVariables.contains(PriceVariable.INPUT)) {
      q.append(tmpVarName).append("\n")
              .append(" |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.INPUT).append(", _time: ").append(start).append(",_measurement: \"").append(targetMeasurement).append("\",_field:\"").append(priceMeasurement).append("\"}))\n")
              .append(" |> to(bucket: \"").append(bucket).append("\")\n\n");
    }

    if(priceVariables != null && priceVariables.contains(PriceVariable.OUTPUT)) {
      q.append(tmpVarName).append("\n")
              .append(" |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.OUTPUT).append(", _time: ").append(start).append(",_measurement: \"").append(targetMeasurement).append("\",_field:\"").append(priceMeasurement).append("2\"}))\n")
              .append(" |> to(bucket: \"").append(bucket).append("\")\n\n");
    }
    return q.toString();
  }

  private String generateTotalSumQueryFromDevices(String systemId,String sourceField,String calcSourceField,String targetFieldDevice,String targetField,
                                                  String bucket,String start,String end,Set<PriceVariable> priceVariables){
    return generateTotalSumQueryFromDevices(systemId,sourceField, calcSourceField, targetFieldDevice, targetField, bucket, start, end,
                                            WsToKwhFactor, priceVariables, null);
  }

  private String generateTotalSumQueryFromDevices(String systemId,
                                                    String sourceField, String calcSourceField, String targetFieldDevice, String targetField,
                                                    String bucket, String start, String end, double multiplier,Set<PriceVariable> priceVariables, String valueFilter){
    InfluxMeasurement sourceMeasurement = InfluxMeasurement.SOLAR_DATA_DEVICE;
    InfluxMeasurement deviceDayMeasurement = InfluxMeasurement.SOLAR_DAY_DATA_DEVICE;

    String multString = decimalFormat.format(multiplier);

    StringBuilder q = new StringBuilder(4096);
    q.append("r1_").append(sourceField).append(" = from(bucket: \"").append(bucket).append("\")\n")
    .append("  |> range(start: ").append(start).append(", stop: ").append(end).append(")\n")
    .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(sourceMeasurement).append("\")\n")
    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
    .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(sourceField).append("\")\n")
    .append("  |> filter(fn: (r) => r[\"_value\"] > 0)\n")
    .append("  |> spread()\n")
    .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"").append(targetFieldDevice).append("\"}))\n")
    .append("  |> to(bucket: \"").append(bucket).append("\")\n\n")

    //this double mapping is needed because it will be sorted by field names so it is used first
    .append("r3_").append(sourceField).append(" = r1_").append(sourceField).append("\n")
    .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"__").append(targetFieldDevice).append("\"}))\n\n");

    if(priceVariables != null && priceVariables.contains(PriceVariable.INPUT)) {
      q.append("r1_price_").append(sourceField).append(" = r1_").append(sourceField).append("\n")
      .append("  |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.INPUT).append(", _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"").append(targetFieldDevice).append("Price\"}))\n")
      .append("  |> to(bucket: \"").append(bucket).append("\")\n\n")

      //this double mapping is needed because it will be sorted by field names so it is used first
      .append("r3_price_").append(sourceField).append(" = r1_price_").append(sourceField).append("\n")
      .append("  |> map(fn: (r) => ({r with _value: r._value, _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"__").append(targetFieldDevice).append("Price\"}))\n\n");
    }

    if(priceVariables != null && priceVariables.contains(PriceVariable.OUTPUT)) {
      q.append("r1_price2_").append(sourceField).append(" = r1_").append(sourceField).append("\n")
      .append("  |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.OUTPUT).append(", _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"").append(targetFieldDevice).append("Price2\"}))\n")
      .append("  |> to(bucket: \"").append(bucket).append("\")\n\n")

      //this double mapping is needed because it will be sorted by field names so it is used first
      .append("r3_price2_").append(sourceField).append(" = r1_price2_").append(sourceField).append("\n")
      .append("  |> map(fn: (r) => ({r with _value: r._value, _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"__").append(targetFieldDevice).append("Price2\"}))\n\n");
    }

    q.append("r2_").append(sourceField).append(" = from(bucket: \"").append(bucket).append("\")\n")
    .append("  |> range(start: ").append(start).append(", stop: ").append(end).append(")\n")
    .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(sourceMeasurement).append("\")\n")
    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
    .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(calcSourceField).append("\" or r[\"_field\"] == \"Duration\")\n")
    .append("  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
    if (valueFilter != null && !valueFilter.isEmpty()) {
        q.append("  |> filter(fn: (r) => r.").append(calcSourceField).append(" ").append(valueFilter).append(")\n");
    }
    q.append("  |> map(fn: (r) => ({r with _value: r.").append(calcSourceField).append(" * ").append(multString).append(" * r.Duration}))\n")
    .append("  |> cumulativeSum()\n")
    .append("  |> max()\n")
    .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"Calc").append(targetFieldDevice).append("\"}))\n")
    .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");

    if(priceVariables != null && priceVariables.contains(PriceVariable.INPUT)) {
        q.append("r2_price_").append(sourceField).append(" = r2_").append(sourceField).append("\n")
        .append("  |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.INPUT).append(", _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"Calc").append(targetFieldDevice).append("Price\"}))\n")
        .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");
    }

    if(priceVariables != null && priceVariables.contains(PriceVariable.OUTPUT)) {
        q.append("r2_price2_").append(sourceField).append(" = r2_").append(sourceField).append("\n")
        .append("  |> map(fn: (r) => ({r with _value: r._value * ").append(PriceVariable.OUTPUT).append(", _time: ").append(start).append(",_measurement: \"").append(deviceDayMeasurement).append("\",_field:\"Calc").append(targetFieldDevice).append("Price2\"}))\n")
        .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");
    }

    q.append("combined_").append(sourceField).append(" = union(tables: [r3_").append(sourceField).append(", r2_").append(sourceField).append("])\n\n")

    .append("combined_").append(sourceField).append("\n")
    .append("  |> group(columns: [\"id\"])\n")
    .append("  |> sort(columns: [\"_field\"], desc: true)\n")//important se commend above sorting
    .append("  |> limit(n: 1)\n")
    .append("  |> group(columns: [\"system\"])\n")
    .append("  |> sum()\n")
    .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(InfluxMeasurement.SOLAR_DAY_DATA).append("\",_field:\"").append(targetField).append("\"}))\n")
    .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");

    if(priceVariables != null && priceVariables.contains(PriceVariable.INPUT)) {
        q.append("combined_price_").append(sourceField).append(" = union(tables: [r3_price_").append(sourceField).append(", r2_price_").append(sourceField).append("])\n\n")

        .append("combined_price_").append(sourceField).append("\n")
        .append("  |> group(columns: [\"id\"])\n")
        .append("  |> sort(columns: [\"_field\"], desc: true)\n")//important se commend above sorting
        .append("  |> limit(n: 1)\n")
        .append("  |> group(columns: [\"system\"])\n")
        .append("  |> sum()\n")
        .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(InfluxMeasurement.SOLAR_DAY_DATA).append("\",_field:\"").append(targetField).append("Price\"}))\n")
        .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");
    }

    if(priceVariables != null && priceVariables.contains(PriceVariable.OUTPUT)) {
      q.append("combined_price2_").append(sourceField).append(" = union(tables: [r3_price2_").append(sourceField).append(", r2_price2_").append(sourceField).append("])\n\n")

              .append("combined_price2_").append(sourceField).append("\n")
              .append("  |> group(columns: [\"id\"])\n")
              .append("  |> sort(columns: [\"_field\"], desc: true)\n")//important se commend above sorting
              .append("  |> limit(n: 1)\n")
              .append("  |> group(columns: [\"system\"])\n")
              .append("  |> sum()\n")
              .append("  |> map(fn: (r) => ({r with _time: ").append(start).append(",_measurement: \"").append(InfluxMeasurement.SOLAR_DAY_DATA).append("\",_field:\"").append(targetField).append("Price2\"}))\n")
              .append("  |> to(bucket: \"").append(bucket).append("\")\n\n");
    }

      return q.toString();

    /*

    var q = "r1_"+sourceField+" = from(bucket: \"test\")\n" +
            "  |> range(start: "+start+", stop: "+end+")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.SOLAR_DATA_DEVICE+"\")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceField+"\")\n" +
            "  |> filter(fn: (r) => r[\"_value\"] > 0)\n" +
            "  |> spread()\n" +
            "\n" +
            "\n" +
            "r2_+"+sourceField+" = from(bucket: \"test\")\n" +
            "  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.SOLAR_DATA_DEVICE+"\")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_field\"] == \""+calcSourceField+"\")\n" +
            "  |> filter(fn: (r) => r[\"_value\"] > 0)\n" +
            "  |> spread()\n" +
            "\n" +
            "combined = union(tables: [r1_"+sourceField+", r2_"+sourceField+"])\n" +
            "\n" +
            "combined\n" +
            "  |> group(columns: [\"id\"])\n" +
            "  |> sort(columns: [\"source\"], desc: true)\n" +
            "  |> limit(n: 1)\n" +
            "  |> group(columns: [\"_field\", \"system\"])\n" +
            "  |> sum()\n" +
            "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetField+"\"}))" +
            "  |> to(bucket: \"" + bucket + "\")\n" +
            (price ?  "   |> map(fn: (r) => ({r with _value: r._value * price, _time: " + start + ",_measurement: \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\",_field:\"" + priceMeasurement + "\"}))\n |> to(bucket: \"" + bucket + "\")\n\n" : "");

    return q;
    */
  }

  private String generatePriceQuery(SolarSystem solarSystem, String start, String end) {
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"InputWatt", InfluxFields.calcProdKWHField.getName(),start,end,
            WsToKwhFactor,Collections.singleton(PriceVariable.INPUT));
  }

  private String generateProductionQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"InputWatt", InfluxFields.calcProdKWHField.getName(),start,end,
        WsToKwhFactor,Collections.singleton(PriceVariable.INPUT));
  }

  private String generateConsumptionQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"OutputWatt",InfluxFields.calcConsKWHField.getName(),
            start,end, WsToKwhFactor,Collections.singleton(PriceVariable.INPUT));
  }

  private String generateBatteryQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"BatteryWatt",InfluxFields.calcBatteryKWHField.getName(),
            start,end,WsToKwhFactor,null);
  }

  private String generateGridConsumptionQuery(SolarSystem solarSystem, String start, String end) {
    return generateSumQuery(solarSystem.getInfluxTagName(), InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(), "GridWatt", InfluxFields.calcGridConsKWHField.getName(),
        start, end, WsToKwhFactor, Collections.singleton(PriceVariable.INPUT), "> 0");
  }

  private String generateGridFeedInQuery(SolarSystem solarSystem, String start, String end) {
    return generateSumQuery(solarSystem.getInfluxTagName(), InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(), "GridWatt", InfluxFields.calcGridFeedInKWHField.getName(),
        start, end, -WsToKwhFactor, Set.of(PriceVariable.OUTPUT,PriceVariable.INPUT), "< 0");
  }

  /* is this here needed ?
  private String generateTotalProductionQueryDC(SolarSystem solarSystem,String start,String end){
      return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,InfluxMeasurement.SOLAR_DAY_DATA,
          solarSystem.getOwnedBy().getInfluxBucketName(),"InputDCTotalKWH",InfluxFields.prodKWHDCField.getName(),start,end,false) +
          generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA_DEVICE,InfluxMeasurement.SOLAR_DAY_DATA_DEVICE,
          solarSystem.getOwnedBy().getInfluxBucketName(),"InputDCTotalKWH",InfluxFields.prodKWHDCField.getName(),start,end,false);
  }

  private String generateProductionQueryDC(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"InputWattDC",InfluxFields.calcProdKWHDCField.getName(),start,end,
            WsToKwhFactor,true);
  }*/

  private String generateTotalProductionQuery(SolarSystem solarSystem,String start,String end){
    return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,InfluxMeasurement.SOLAR_DAY_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"InputTotalKWH",InfluxFields.prodKWHField.getName(),start,end,Collections.singleton(PriceVariable.INPUT)) +

            generateTotalSumQueryFromDevices(solarSystem.getInfluxTagName(),"InputTotalKWH","InputWatt",InfluxFields.prodKWHField.toString(), InfluxFields.calcByDevicesProdKWHField.toString(),
                    solarSystem.getOwnedBy().getInfluxBucketName(),start,end,Collections.singleton(PriceVariable.INPUT));
  }

  private String generateTotalBatteryQuery(SolarSystem solarSystem,String start,String end){
    return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,InfluxMeasurement.SOLAR_DAY_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"BatteryTotalKWH",InfluxFields.batteryKWHField.getName(),start,end,null) +

        generateTotalSumQueryFromDevices(solarSystem.getInfluxTagName(),"BatteryTotalKWH","BatteryWatt",InfluxFields.batteryKWHField.toString(), InfluxFields.calcByDevicesBatteryKWHField.toString(),
                solarSystem.getOwnedBy().getInfluxBucketName(),start,end,null);
  }

  private String generateTotalConsumptionQuery(SolarSystem solarSystem,String start,String end){
      return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,InfluxMeasurement.SOLAR_DAY_DATA,
            solarSystem.getOwnedBy().getInfluxBucketName(),"OutputTotalKWH",InfluxFields.consKWHField.getName(),start,end,Collections.singleton(PriceVariable.INPUT)) +

          generateTotalSumQueryFromDevices(solarSystem.getInfluxTagName(),"OutputTotalKWH","OutputWatt",InfluxFields.consKWHField.toString(),InfluxFields.calcByDevicesConsKWHField.toString(),
            solarSystem.getOwnedBy().getInfluxBucketName(),start,end,Collections.singleton(PriceVariable.INPUT));
  }

  private String generateTotalGridConsumptionQuery(SolarSystem solarSystem, String start, String end) {
     return generateTotalSumQuery(solarSystem.getInfluxTagName(), InfluxMeasurement.SOLAR_DATA, InfluxMeasurement.SOLAR_DAY_DATA,
              solarSystem.getOwnedBy().getInfluxBucketName(), "GridTotalConsumptionKWH", InfluxFields.gridConsKWHField.getName(), start, end, Collections.singleton(PriceVariable.INPUT)) +

             generateTotalSumQueryFromDevices(solarSystem.getInfluxTagName(), "GridTotalConsumptionKWH", "GridWatt", InfluxFields.gridConsKWHField.toString(),InfluxFields.calcByDevicesGridConsKWHField.toString(),
              solarSystem.getOwnedBy().getInfluxBucketName(), start, end, WsToKwhFactor, Collections.singleton(PriceVariable.INPUT), "> 0");
  }

  private String generateTotalGridFeedInQuery(SolarSystem solarSystem, String start, String end) {
      return generateTotalSumQuery(solarSystem.getInfluxTagName(), InfluxMeasurement.SOLAR_DATA, InfluxMeasurement.SOLAR_DAY_DATA,
              solarSystem.getOwnedBy().getInfluxBucketName(), "GridTotalFeedInKWH", InfluxFields.gridFeedInKWHField.getName(), start, end, Set.of(PriceVariable.OUTPUT,PriceVariable.INPUT)) +

             generateTotalSumQueryFromDevices(solarSystem.getInfluxTagName(),"GridTotalFeedInKWH", "GridWatt", InfluxFields.gridFeedInKWHField.toString(), InfluxFields.calcByDevicesGridFeedInKWHField.toString(),
              solarSystem.getOwnedBy().getInfluxBucketName(), start, end, -WsToKwhFactor, Set.of(PriceVariable.OUTPUT,PriceVariable.INPUT), "< 0");
  }

  String generateDefaultQuery(SolarSystem solarSystem,String start, String end){
    StringBuilder q = new StringBuilder(4096);
    q.append("getFieldValue = (tables=<-) => {\n")
            .append("extract = tables\n")
            .append("        |> findColumn(fn: (key) => true, column: \"_value\")\n")
            .append("\n")
            .append("return if length(arr: extract) == 0 then 0.0 else extract[0]\n")
            .append("}\n")
        .append(PriceVariable.INPUT).append(" = from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
        .append("  |> range(start: 0, stop: ").append(end).append(")\n")
        .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(InfluxMeasurement.SELDOM_CHANGING_STATS).append("\")\n")
        .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
        .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(InfluxFields.energyPriceMeasurement.getName()).append("\")\n")
        .append("  |> last()\n")
        .append("  |> getFieldValue()\n\n")

        .append(PriceVariable.OUTPUT).append("= from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
        .append("  |> range(start: 0, stop: ").append(end).append(")\n")
        .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(InfluxMeasurement.SELDOM_CHANGING_STATS).append("\")\n")
        .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
        .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(InfluxFields.energyPriceFeedInMeasurement.getName()).append("\")\n")
        .append("  |> last()\n")
        .append("  |> getFieldValue()\n\n")

      //generatePriceQuery(solarSystem,start,end) +
      .append(generateProductionQuery(solarSystem,start,end))
      //generateProductionQueryDC(solarSystem,start,end) +
      .append(generateTotalProductionQuery(solarSystem,start,end))
      //generateTotalProductionQueryDC(solarSystem,start,end) +
      .append(generateBatteryQuery(solarSystem,start,end))
      .append(generateTotalBatteryQuery(solarSystem,start,end))
      .append(generateConsumptionQuery(solarSystem,start,end))
      .append(generateTotalConsumptionQuery(solarSystem,start,end))
      .append(generateGridConsumptionQuery(solarSystem,start,end))
      .append(generateTotalGridConsumptionQuery(solarSystem,start,end))
      .append(generateGridFeedInQuery(solarSystem,start,end))
      .append(generateTotalGridFeedInQuery(solarSystem,start,end));
    return q.toString();
  }

  public void deleteAllDayData(SolarSystem solarSystem){
    influxConnection.getClient().getDeleteApi().delete(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),OffsetDateTime.now(),"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ solarSystem.getInfluxTagName()+"\"",solarSystem.getOwnedBy().getInfluxBucketName(),"my-org");
  }

  public void deleteAllDayData(SolarSystem solarSystem,OffsetDateTime from,OffsetDateTime to){
    influxConnection.getClient().getDeleteApi().delete(from,to,"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ solarSystem.getInfluxTagName()+"\"","user-"+ solarSystem.getOwnedBy().getInfluxBucketName(),"my-org");
  }

  public boolean runInitial(SolarSystem solarSystem, ThreadPoolExecutor threadPoolExecutor){

    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var now = ZonedDateTime.now();
    boolean skipQuery = solarSystem.getLastManualCalculation() != null && solarSystem.getLastManualCalculation() > now.minusDays(1).toInstant().toEpochMilli() && !user.getIsAdmin();
    if(!user.getIsAdmin()){
      solarSystemRepository.updateLastManualCalculation(solarSystem.getId(),now.toInstant().toEpochMilli());
    }

    new Thread(()->{
      try {
        deleteAllDayData(solarSystem);
        runInitial(solarSystem, null, skipQuery,threadPoolExecutor);
        runUpdateTotalValues(solarSystem);
      }catch (Exception e){
        LOG.error("Error on updating daily values: {}",e.getMessage(),e);
      }
    }).start();

    return !skipQuery;
  }

  public void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked){
    runInitial(solarSystem,lastChecked,false);
  }

  private void executeQueryWithRetry(String query){
      Exception lastException = null;
      for(int i=1;i<=5;i++){
          try {
              influxConnection.getClient().getQueryApi().query(query);
              return;
          }catch (RequestTimeoutException ex){
              lastException = ex;
              LOG.warn("Influx Query timed out, retry: "+(i-1));
              LOG.debug("Influx Query timed out, retry: "+(i-1) + ex.getMessage());
          }
          try {
              Thread.sleep((long) (Math.pow(i,2) * 1000));
          } catch (InterruptedException e) {
              lastException = e;
              break;
          }
      }
      throw new RuntimeException(lastException);
  }

  public void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked,boolean skipQuery) {
      runInitial(solarSystem,lastChecked,skipQuery,null);
  }

  public void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked,boolean skipQuery,ThreadPoolExecutor threadPoolExecutor){

    if(lastChecked == null) {
      LOG.info("Running full day generation with skip {} for system {} with id {}", skipQuery,solarSystem.getName(), solarSystem.getId());
    }else{
      LOG.info("Running day generation for system {} with id {} from {}", solarSystem.getName(), solarSystem.getId(),lastChecked);
    }

    var zId = ZoneId.of(solarSystem.getTimezone());

    //Date date = Date.from(instant);
    formatter.setTimeZone(TimeZone.getTimeZone(zId));

    ZonedDateTime s;

    //var s = solarSystem.getCreationDate().toLocalDate().atStartOfDay(zId);

    boolean fullCalculatoin = false;
    if(lastChecked != null){
      //s = ZonedDateTime.ofInstant(lastChecked.toInstant(),zId).toLocalDate().atStartOfDay(zId);
      s = lastChecked;
    }else{
      fullCalculatoin = true;
      var startDate = influxConnection.getFirstDataEver(solarSystem);
      if(startDate == null){
        LOG.warn("No day generation possible for system {} with id {} from {} because no data in influx", solarSystem.getName(), solarSystem.getId(),lastChecked);
        return;
      }else{
        LOG.info("Start date for full generation for system {} with id {} is: {}",  solarSystem.getName(), solarSystem.getId(), startDate);
      }
      s = startDate.atZone(zId);
    }

    s= s.withHour(0).withMinute(0).withSecond(0).withNano(0);

    /*Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zId));
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);*/

    if(lastChecked != null){
      //cal.add(Calendar.DATE, 1);
      s = s.plusDays(1);
    }

    //if(s.get(Calendar.HOUR_OF_DAY) != 0){
    if(s.getHour() != 0){
      //System.out.println(cal.get(Calendar.HOUR_OF_DAY));
      LOG.error("Error start hour offset not not 0 it is {} instead -> skipped writing to database",s.getHour());
      return;
    }

    //var startToday = ZonedDateTime.now(zId);
    var startToday = ZonedDateTime.now(zId);
    startToday = startToday.withHour(0).withMinute(0).withSecond(0).withNano(0);
    startToday = startToday.plusDays(2);

    List<Future<?>> tasks = new ArrayList<>();

    while(true){
      //var starttest = formatter.format(cal.getTime());
      var start = zoneFormatter.format(s);
      //cal.add(Calendar.DATE, 2);
      s = s.plusDays(1);
      //if(cal.getTimeInMillis() > new Date().getTime()){
      if(s.isAfter(startToday.minusSeconds(1))){
        break;
      }
      //s = s.minusDays(1);
      var end = zoneFormatter.format(s);
      if(!skipQuery) {
        var query = generateDefaultQuery(solarSystem, start, end);
        if(fullCalculatoin && threadPoolExecutor != null){
            tasks.add(threadPoolExecutor.submit(()->{
                executeQueryWithRetry(query);
                LOG.info("Updated Day data for System {} from {} to {} in ThreadPool", solarSystem.getId(), start, end);
            }));
        }else {
            influxConnection.getClient().getQueryApi().query(query);
            LOG.info("Updated Day data for System {} from {} to {}", solarSystem.getId(), start, end);
        }
      }
    }

    // Wait for all tasks
    for(int i=0;i<tasks.size();i++){
        try {
            tasks.get(i).get(); // blocks until task is done
        } catch (InterruptedException | ExecutionException e) {
            return;//shutdown signal
        }
        LOG.info("Full Update of system {} in progress: {} of {} days done", solarSystem.getId(), i, tasks.size());
    }

    s = s.minusDays(2);
    //cal.add(Calendar.DATE, -3);
    //var time = ZonedDateTime.ofInstant(cal.toInstant(),cal.getTimeZone().toZoneId());
    long time = s.toInstant().toEpochMilli();
    solarSystemRepository.updateLastCalculation(solarSystem.getId(),time);

    if(lastChecked == null){
      LOG.info("Full generation system {} with id {} finished!",  solarSystem.getName(), solarSystem.getId());
    }
  }

  public void runUpdateLastDays(SolarSystem solarSystem,ZonedDateTime day){
    var zId = ZoneId.of(solarSystem.getTimezone());
    //Date date = Date.from(instant);

    //this will sate date to midnight (if not already done before)
    var s = ZonedDateTime.ofInstant(day.toInstant(),zId).toLocalDate().atStartOfDay(zId);

    if(s.getHour() != 0){
      LOG.error("Error start hour offset not not 0 it is {} instead -> skipped writing to database",s.getHour());
      return;
    }

    var start = zoneFormatter.format(s);
    s = s.plusDays(1);
    var end = zoneFormatter.format(s);
    var query = generateDefaultQuery(solarSystem,start,end);
    /*cal.add(Calendar.MILLISECOND, -1);
    end = formatter.format(cal.getTime());
    deleteAllDayData(solarSystem,OffsetDateTime.parse(start),OffsetDateTime.parse(end));
    cal.add(Calendar.MILLISECOND, 1);
    end = formatter.format(cal.getTime());*/
    var res = influxConnection.getClient().getQueryApi().query(query);
    LOG.info("Updated Day data for System {} from {} to {}", solarSystem.getId(),start,end);
  }


  public void runUpdateTotalValues(SolarSystem solarSystem){
    LOG.info("Updating total values for system: {}",solarSystem.getId());

    Set<String> totalFilter = (solarSystem.getViewData() != null && solarSystem.getViewData().getTotalFilter() != null)
            ? solarSystem.getViewData().getTotalFilter()
            : Collections.emptySet();

    // Use pregenerated default query when no filters are present
    String totalQuery = totalFilter.isEmpty()
            ? defaultTotalQuery
            : generateTotalQuery(totalFilter);

    var end = zoneFormatter.format(ZonedDateTime.now());
    var query = "from(bucket: \""+solarSystem.getOwnedBy().getInfluxBucketName()+"\")\n"
            + "  |> range(start: 0, stop: "+end+")\n"
            + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.SOLAR_DAY_DATA+"\")\n"
            + "  |> filter(fn: (r) => r[\"system\"] == \""+solarSystem.getInfluxTagName()+"\")\n"
            + totalQuery;

    var results = influxConnection.getClient().getQueryApi().query(query);

    if(results.isEmpty()){
      LOG.warn("Empty result on runUpdateTotalValues");
      return;
    }

    TotalValues totalValues = new TotalValues();

    Float difSumFeedInPrice = null;

    for (FluxTable res : results) {
        if (res.getRecords().size() != 1) {
            LOG.warn("result records not single on runUpdateTotalValues");
            return;
        }

        for (FluxRecord record : res.getRecords()) {
            var obj = record.getValueByKey("TotalProducedKWH");
            if (obj != null) {
                totalValues.setProducedKWH(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalConsumedKWH");
            if (obj != null) {
                totalValues.setConsumedKWH(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalGridConsumedKWH");
            if (obj != null) {
                totalValues.setGridConsumedKWH(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalGridFeedInKWH");
            if (obj != null) {
                totalValues.setGridFeedInKWH(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalProducedKWHPrice");
            if (obj != null) {
                totalValues.setProducedKWHPrice(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalConsumedKWHPrice");
            if (obj != null) {
                totalValues.setConsumedKWHPrice(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalGridConsumedKWHPrice");
            if (obj != null) {
                totalValues.setGridConsumedKWHPrice(((Number) obj).floatValue());
            }

            obj = record.getValueByKey("TotalGridFeedInKWHPrice");
            if (obj != null) {
              difSumFeedInPrice = ((Number) obj).floatValue();
            }

            obj = record.getValueByKey("TotalGridFeedInKWHPrice2");
            if (obj != null) {
                totalValues.setGridFeedInKWHPrice(((Number) obj).floatValue());
            }
        }
    }

    if(totalValues.getGridConsumedKWH() != null){
      if(totalValues.getGridFeedInKWH() == null){
        totalValues.setGridFeedInKWH(0f);
      }
    }

    if(totalValues.getGridFeedInKWH() != null){
      if(totalValues.getGridConsumedKWH() == null){
        totalValues.setGridConsumedKWH(0f);
      }
    }

    Float calCons = null;
    if(totalValues.getConsumedKWH() != null) {
        calCons = totalValues.getConsumedKWH();
    }
    if(calCons != null && totalValues.getGridFeedInKWH() != null){
        calCons -=  totalValues.getGridFeedInKWH();
        if(calCons < 0){
            calCons = 0f;Float calcConsumedKWHPrice;
        }
    }
    if(totalValues.getGridConsumedKWH() != null){
        if(calCons == null) {
            calCons = 0.f;
        }
        calCons += totalValues.getGridConsumedKWH();
    }
    totalValues.setCalcConsumedKWH(calCons);


    Float calConsPrice = null;
    if(totalValues.getConsumedKWHPrice() != null) {
        calConsPrice = totalValues.getConsumedKWHPrice();
    }
    if(calConsPrice != null && difSumFeedInPrice != null){
        calConsPrice -=  difSumFeedInPrice;
        if(calConsPrice < 0){
            calConsPrice = 0f;
        }
    }
    totalValues.setCalcConsumedKWHPrice(calConsPrice);

    solarSystemRepository.updateTotalValues(solarSystem.getId(),totalValues);
  }
}
