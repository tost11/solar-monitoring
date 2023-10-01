package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.TotalValues;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import jakarta.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class InfluxTaskService {

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  @Autowired
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private InfluxConnection influxConnection;

  public static final String calcProdKWHField = "CalcProducedKWH";
  public static final String calcProdKWHDCField = "CalcProducedKWHDC";
  public static final String calcConsKWHField = "CalcConsumedKWH";
  public static final String calcBatteryKWHField = "CalcBatteryKWH";

  public static final String prodKWHField = "ProducedKWH";
  public static final String prodKWHDCField = "ProducedKWHDC";
  public static final String consKWHField = "ConsumedKWH";
  public static final String batteryKWHField = "BatteryKWH";

  //public static final String prodKWHFieldSum = "ProducedKWH_sum";
  //public static final String prodKWHDCFieldSum = "ProducedKWHDC_sum";
  //public static final String consKWHFieldSum = "ConsumedKWH_sum";
  //public static final String batteryKWHFieldSum = "BatteryKWH_sum";

  private final double WsToKwhFactor = 0.000277778 / 1000;

  DecimalFormat decimalFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.US));

  final String yesterdayStartTime = "experimental.addDuration(d: -1d, to: today())";
  final String todayStartTime = "today()";

  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
  DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  @PostConstruct
  private void init(){
    decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
  }

  private String generateSumQuery(String systemId,InfluxMeasurement influxMeasurement,String bucket,String sourceMeasurement,String targetMeasurement,String start,String end,double multiplier,boolean price){
    String multString = decimalFormat.format(multiplier);

    String priceMeasurement = targetMeasurement+"Price";

    return "from(bucket: \""+bucket+"\")\n"
      + "  |> range(start: " + start + ", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\" or r[\"_field\"] == \"Duration\")\n"
      + "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
      + "  |> map(fn: (r) => ({r with _value: r."+sourceMeasurement+" * " + multString + " * r.Duration}))\n"
      + "  |> cumulativeSum()\n"
      + "  |> max()\n"
      + "  |> map(fn: (r) => ({r with _value: r._value, _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"" + bucket + "\")\n"
      + (price ?  "   |> map(fn: (r) => ({r with _value: r._value * price, _time: " + start + ",_measurement: \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\",_field:\"" + priceMeasurement + "\"}))\n |> to(bucket: \"" + bucket + "\")\n\n" : "");
  }

  private String generateTotalSumQuery(String systemId,InfluxMeasurement influxMeasurement,String bucket,String sourceMeasurement,String targetMeasurement,String start,String end,boolean useId,boolean price){

    String priceMeasurement = targetMeasurement+"Price";

    var q = "from(bucket: \"" + bucket + "\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\")\n"
      + (useId ? "|> filter(fn: (r) => r[\"id\"] == \"0\")\n" : "")
      + "  |> spread() "
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"" + bucket + "\")\n"
      + (price ?  "   |> map(fn: (r) => ({r with _value: r._value * price, _time: " + start + ",_measurement: \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\",_field:\"" + priceMeasurement + "\"}))\n |> to(bucket: \"" + bucket + "\")\n\n" : "");

    if(useId){
      q += "from(bucket: \"" + bucket +"\")\n"
        + "  |> range(start: "+start+", stop: "+end+")\n"
        + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
        + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
        + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\")\n"
        + "  |> filter(fn: (r) => r[\"id\"] != \"0\")\n"
        + "  |> spread() "
        + "  |> group(columns: [\"system\",\"type\"],  mode:\"by\")\n"
        + "  |> sum()\n"
        + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"_sum\"}))\n"
        + "  |> to(bucket: \"" + bucket +"\")\n\n";
    }
    return q;
  }

  private String generateProductionQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"InputWatt",calcProdKWHField,start,end,
        WsToKwhFactor,true);
  }

  private String generateProductionQueryDC(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"InputWattDC",calcProdKWHDCField,start,end,
        WsToKwhFactor,true);
  }

  private String generateTotalProductionQuery(SolarSystem solarSystem,String start,String end){
      return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
          solarSystem.getOwnedBy().getInfluxBucketName(),"InputTotalKWH",prodKWHField,start,end,false,true);
  }

  private String generateTotalProductionQueryDC(SolarSystem solarSystem,String start,String end){
      return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
          solarSystem.getOwnedBy().getInfluxBucketName(),"InputDCTotalKWH",prodKWHDCField,start,end,false,false);
  }

  private String generateBatteryQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"BatteryWatt",calcBatteryKWHField,start,end,WsToKwhFactor,false);
  }

  private String generateTotalBatteryQuery(SolarSystem solarSystem,String start,String end){
    return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"BatteryTotalKWH",batteryKWHField,start,end,
        false,false);
  }

  private String generateConsumptionQuery(SolarSystem solarSystem,String start,String end){
    return generateSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
        solarSystem.getOwnedBy().getInfluxBucketName(),"OutputWatt",calcConsKWHField,start,end,
            WsToKwhFactor,true);
  }

  private String generateTotalConsumptionQuery(SolarSystem solarSystem,String start,String end){
      return generateTotalSumQuery(solarSystem.getInfluxTagName(),InfluxMeasurement.SOLAR_DATA,
          solarSystem.getOwnedBy().getInfluxBucketName(),"OutputTotalKWH",consKWHField,start,end,false,true);
  }

  String generateDefaultQuery(SolarSystem solarSystem,String start, String end){
    String q = "getFieldValue = (tables=<-) => {\n" +
            "extract = tables\n" +
            "        |> findColumn(fn: (key) => true, column: \"_value\")\n" +
            "\n" +
            "return if length(arr: extract) == 0 then 0.0 else extract[0]\n" +
            "}\n" +
        "price = from(bucket: \""+solarSystem.getOwnedBy().getInfluxBucketName()+"\")\n"
      + "  |> range(start: 0, stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.SELDOM_CHANGING_STATS+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+solarSystem.getInfluxTagName()+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+InfluxService.energyPriceMeasurement+"\")\n"
      + "  |> last()\n"
      + "  |> getFieldValue()\n\n" +

      generateProductionQuery(solarSystem,start,end) +
      generateProductionQueryDC(solarSystem,start,end) +
      generateTotalProductionQuery(solarSystem,start,end) +
      generateTotalProductionQueryDC(solarSystem,start,end) +
      generateBatteryQuery(solarSystem,start,end) +
      generateTotalBatteryQuery(solarSystem,start,end) +
      generateConsumptionQuery(solarSystem,start,end) +
      generateTotalConsumptionQuery(solarSystem,start,end);
    return q;
  }

  public void deleteAllDayData(SolarSystem solarSystem){
    influxConnection.getClient().getDeleteApi().delete(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),OffsetDateTime.now(),"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ solarSystem.getInfluxTagName()+"\"",solarSystem.getOwnedBy().getInfluxBucketName(),"my-org");
  }

  public void deleteAllDayData(SolarSystem solarSystem,OffsetDateTime from,OffsetDateTime to){
    influxConnection.getClient().getDeleteApi().delete(from,to,"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ solarSystem.getInfluxTagName()+"\"","user-"+ solarSystem.getOwnedBy().getInfluxBucketName(),"my-org");
  }

  public boolean runInitial(SolarSystem solarSystem){

    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var now = ZonedDateTime.now();
    boolean skipQuery = solarSystem.getLastManualCalculation() != null && solarSystem.getLastManualCalculation() > now.minusDays(1).toInstant().toEpochMilli() && !user.getIsAdmin();
    if(!user.getIsAdmin()){
      solarSystemRepository.updateLastManualCalculation(solarSystem.getId(),now.toInstant().toEpochMilli());
    }

    //TODO refactor to thread-pool
    new Thread(()->{
      deleteAllDayData(solarSystem);
      runInitial(solarSystem,null,skipQuery);
      runUpdateTotalValues(solarSystem);
    }).start();

    return !skipQuery;
  }

  private void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked){
    runInitial(solarSystem,lastChecked,false);
  }

  private void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked,boolean skipQuery){

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

    if(lastChecked != null){
      //s = ZonedDateTime.ofInstant(lastChecked.toInstant(),zId).toLocalDate().atStartOfDay(zId);
      s = lastChecked;
    }else{
      var startDate = influxConnection.getFirstDataEver(solarSystem);
      if(startDate == null){
        LOG.debug("No day generation possible for system {} with id {} from {} because no data in influx", solarSystem.getName(), solarSystem.getId(),lastChecked);
        return;
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
        influxConnection.getClient().getQueryApi().query(query);
        LOG.info("Updated Day data for System {} from {} to {}", solarSystem.getId(),start,end);
      }
    }


    s = s.minusDays(2);
    //cal.add(Calendar.DATE, -3);
    //var time = ZonedDateTime.ofInstant(cal.toInstant(),cal.getTimeZone().toZoneId());
    long time = s.toInstant().toEpochMilli();
    solarSystemRepository.updateLastCalculation(solarSystem.getId(),time);
  }

  @Scheduled(fixedDelayString = "${timing.updateDayData:900000}",initialDelayString = "${timing.delayDayData:0}")//check every 15 minutes
  public void updateDayData(){

    LOG.info("Running updateDayData scheduler (every 15 min)");

    //ZonedDateTime before = ZonedDateTime.ofInstant(calendar.toInstant(),ZoneId.of("UTC"));

    var list = solarSystemRepository.findAllByLastCalculationIsNull();
    for (var solarSystem : list) {
      runInitial(solarSystem, solarSystem.getLastCalculation() != null ? ZonedDateTime.ofInstant(Instant.ofEpochMilli(solarSystem.getLastCalculation()),ZoneId.of(solarSystem.getTimezone())):null);
    }

    var before = ZonedDateTime.now();
    before = before.minusDays(2).minusHours(22);

    list = solarSystemRepository.findAllByLastCalculationIsLessThan(before.toInstant().toEpochMilli());
    for (var solarSystem : list) {
      runInitial(solarSystem, ZonedDateTime.ofInstant(Instant.ofEpochMilli(solarSystem.getLastCalculation()),ZoneId.of(solarSystem.getTimezone())));
    }
  }

  //TODO move to own microservice
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
    influxConnection.getClient().getQueryApi().query(query);
    LOG.info("Updated Day data for System {} from {} to {}", solarSystem.getId(),start,end);
  }

  //TODO move to microservice
  @Scheduled(fixedDelay = 60*1000*5,initialDelay = 20 * 1000)
  private void updateStatistics(){

    //TODO paging
    var solarSystems = solarSystemRepository.findAllByNeedsStatisticRecalculation(true);
    for (SolarSystem solarSystem : solarSystems) {
      try {
        var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
        var today = ZonedDateTime.now(zId);
        today = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        runUpdateLastDays(solarSystem, today);
        var yesterday = today.minusDays(1);
        runUpdateLastDays(solarSystem, yesterday);
        runUpdateTotalValues(solarSystem);
      }catch (Exception exception){
        LOG.error("Exception on processing last two day solar statistic update");
        exception.printStackTrace();
      }
    }
  }

  public void runUpdateTotalValues(SolarSystem solarSystem){
    LOG.info("Updating total values for system: {}",solarSystem.getId());

    var end = zoneFormatter.format(ZonedDateTime.now());
    var query = "from(bucket: \""+solarSystem.getOwnedBy().getInfluxBucketName()+"\")\n"
            + "  |> range(start: 0, stop: "+end+")\n"
            + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.SOLAR_DAY_DATA+"\")\n"
            + "  |> filter(fn: (r) => r[\"system\"] == \""+solarSystem.getInfluxTagName()+"\")\n"
            + "  |> filter(fn: (r) => \n"
            + "     r[\"_field\"] == \""+calcProdKWHField+"\" or\n"
            + "     r[\"_field\"] == \""+calcConsKWHField+"\" or\n"
            + "     r[\"_field\"] == \""+prodKWHField+"\" or\n"
            + "     r[\"_field\"] == \""+consKWHField+"\" or\n"
            + "     r[\"_field\"] == \""+calcProdKWHField+"Price\" or\n"
            + "     r[\"_field\"] == \""+calcConsKWHField+"Price\" or\n"
            + "     r[\"_field\"] == \""+prodKWHField+"Price\" or\n"
            + "     r[\"_field\"] == \""+consKWHField+"Price\")\n"
            + "  |> cumulativeSum()\n"
            + "  |> last()\n";

    var results = influxConnection.getClient().getQueryApi().query(query);

    if(results.isEmpty()){
      LOG.warn("Empty result on runUpdateTotalValues");
      return;
    }

    TotalValues totalValues = new TotalValues();

    for (FluxTable res : results) {
      if(res.getRecords().size() != 1){
        LOG.warn("result records not single on runUpdateTotalValues");
        return;
      }

      var values = res.getRecords().get(0).getValues();
      if(StringUtils.equals((String)values.get("_field"),calcProdKWHField)){
        totalValues.setCalcProducedKWH(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),calcConsKWHField)){
        totalValues.setCalcConsumedKWH(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),prodKWHField)){
        totalValues.setProducedKWH(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),consKWHField)){
        totalValues.setConsumedKWH(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),calcProdKWHField+"Price")){
        totalValues.setCalcProducedKWHPrice(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),calcConsKWHField+"Price")){
        totalValues.setCalcConsumedKWHPrice(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),prodKWHField+"Price")){
        totalValues.setProducedKWHPrice(((Number)values.get("_value")).floatValue());
      }
      if(StringUtils.equals((String)values.get("_field"),consKWHField+"Price")){
        totalValues.setConsumedKWHPrice(((Number)values.get("_value")).floatValue());
      }
    }
    solarSystemRepository.updateTotalValues(solarSystem.getId(),totalValues);
  }


}
