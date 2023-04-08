package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import jakarta.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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
  private Neo4jSolarSystemRepository neo4jSolarSystemRepository;

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

  public static final String prodKWHFieldSum = "ProducedKWH_sum";
  public static final String prodKWHDCFieldSum = "ProducedKWHDC_sum";
  public static final String consKWHFieldSum = "ConsumedKWH_sum";
  public static final String batteryKWHFieldSum = "BatteryKWH_sum";

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

  private String generateSumQuery(long systemId,InfluxMeasurement influxMeasurement,long userId,String sourceMeasurement,String targetMeasurement,String start,String end,double multiplier){
    String multString = decimalFormat.format(multiplier);

    return "from(bucket: \"user-"+userId+"\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\" or r[\"_field\"] == \"Duration\")\n"
      + "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
      + "  |> map(fn: (r) => ({r with _value: r."+sourceMeasurement+" * " + multString + " * r.Duration}))\n"
      + "  |> cumulativeSum()\n"
      + "  |> max()\n"
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"user-" + userId + "\")\n\n";
  }

  private String generateTotalSumQuery(long systemId,InfluxMeasurement influxMeasurement,long userId,String sourceMeasurement,String targetMeasurement,String start,String end,boolean useId){
    var q = "from(bucket: \"user-"+userId+"\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\")\n"
      + (useId ? "|> filter(fn: (r) => r[\"id\"] == \"0\")\n" : "")
      + "  |> spread() "
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"user-"+userId+"\")\n\n";

    if(useId){
      q += "from(bucket: \"user-"+userId+"\")\n"
        + "  |> range(start: "+start+", stop: "+end+")\n"
        + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
        + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
        + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\")\n"
        + "  |> filter(fn: (r) => r[\"id\"] != \"0\")\n"
        + "  |> spread() "
        + "  |> group(columns: [\"system\",\"type\"],  mode:\"by\")\n"
        + "  |> sum()\n"
        + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \""+InfluxMeasurement.SOLAR_DAY_DATA+"\",_field:\""+targetMeasurement+"_sum\"}))\n"
        + "  |> to(bucket: \"user-"+userId+"\")\n\n";
    }
    return q;
  }

  private String generateProductionQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
    return generateSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
        neo4jSolarSystem.getRelationOwnedBy().getId(),"InputWatt",calcProdKWHField,start,end,
        WsToKwhFactor);
  }

  private String generateProductionQueryDC(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
    return generateSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
        neo4jSolarSystem.getRelationOwnedBy().getId(),"InputWattDC",calcProdKWHDCField,start,end,
        WsToKwhFactor);
  }

  private String generateTotalProductionQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
      return generateTotalSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
          neo4jSolarSystem.getRelationOwnedBy().getId(),"InputTotalKWH",prodKWHField,start,end,false);
  }

  private String generateTotalProductionQueryDC(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
      return generateTotalSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
          neo4jSolarSystem.getRelationOwnedBy().getId(),"InputDCTotalKWH",prodKWHDCField,start,end,false);
  }

  private String generateBatteryQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
    return generateSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
        neo4jSolarSystem.getRelationOwnedBy().getId(),"BatteryWatt",calcBatteryKWHField,start,end,WsToKwhFactor);
  }

  private String generateTotalBatteryQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
    return generateTotalSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
        neo4jSolarSystem.getRelationOwnedBy().getId(),"BatteryTotalKWH",batteryKWHField,start,end,
        false);
  }

  private String generateConsumptionQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
    return generateSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
        neo4jSolarSystem.getRelationOwnedBy().getId(),"OutputWatt",calcConsKWHField,start,end,
            WsToKwhFactor);
  }

  private String generateTotalConsumptionQuery(Neo4jSolarSystem neo4jSolarSystem,String start,String end){
      return generateTotalSumQuery(neo4jSolarSystem.getId(),InfluxMeasurement.SOLAR_DATA,
          neo4jSolarSystem.getRelationOwnedBy().getId(),"OutputTotalKWH",consKWHField,start,end,false);
  }

  String generateDefaultQuery(Neo4jSolarSystem neo4jSolarSystem,String start, String end){
    return "" +
      generateProductionQuery(neo4jSolarSystem,start,end) +
      generateProductionQueryDC(neo4jSolarSystem,start,end) +
      generateTotalProductionQuery(neo4jSolarSystem,start,end) +
      generateTotalProductionQueryDC(neo4jSolarSystem,start,end) +
      generateBatteryQuery(neo4jSolarSystem,start,end) +
      generateTotalBatteryQuery(neo4jSolarSystem,start,end) +
      generateConsumptionQuery(neo4jSolarSystem,start,end) +
      generateTotalConsumptionQuery(neo4jSolarSystem,start,end);
  }

  public void deleteAllDayData(Neo4jSolarSystem neo4jSolarSystem){
    influxConnection.getClient().getDeleteApi().delete(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),OffsetDateTime.now(),"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ neo4jSolarSystem.getId()+"\"","user-"+ neo4jSolarSystem.getRelationOwnedBy().getId(),"my-org");
  }

  public void deleteAllDayData(Neo4jSolarSystem neo4jSolarSystem,OffsetDateTime from,OffsetDateTime to){
    influxConnection.getClient().getDeleteApi().delete(from,to,"_measurement=\""+InfluxMeasurement.SOLAR_DAY_DATA+"\" AND system=\""+ neo4jSolarSystem.getId()+"\"","user-"+ neo4jSolarSystem.getRelationOwnedBy().getId(),"my-org");
  }

  public boolean runInitial(Neo4jSolarSystem neo4jSolarSystem){

    var user = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if(neo4jSolarSystem.getLastManualCalculation() != null && neo4jSolarSystem.getLastManualCalculation().isAfter(ZonedDateTime.now().minusDays(1)) &&
            !user.getIsAdmin()){
      return false;
    }
    if(!user.getIsAdmin()){
      neo4jSolarSystemRepository.updateLastManualCalculation(neo4jSolarSystem.getId(),ZonedDateTime.now());
    }

    new Thread(()->{
      deleteAllDayData(neo4jSolarSystem);
      runInitial(neo4jSolarSystem,null);
    }).start();

    return true;
  }

  private void runInitial(Neo4jSolarSystem neo4jSolarSystem,ZonedDateTime lastChecked){

    if(lastChecked == null) {
      LOG.info("Running full day generation for system {} with id {}", neo4jSolarSystem.getName(), neo4jSolarSystem.getId());
    }else{
      LOG.info("Running day generation for system {} with id {} from {}", neo4jSolarSystem.getName(), neo4jSolarSystem.getId(),lastChecked);
    }

    var zId = ZoneId.of(neo4jSolarSystem.getTimezone());

    //Date date = Date.from(instant);
    formatter.setTimeZone(TimeZone.getTimeZone(zId));

    ZonedDateTime s;

    //var s = solarSystem.getCreationDate().toLocalDate().atStartOfDay(zId);

    if(lastChecked != null){
      //s = ZonedDateTime.ofInstant(lastChecked.toInstant(),zId).toLocalDate().atStartOfDay(zId);
      s = lastChecked;
    }else{
      var startDate = influxConnection.getFirstDataEver(neo4jSolarSystem);
      if(startDate == null){
        LOG.debug("No day generation possible for system {} with id {} from {} because no data in influx", neo4jSolarSystem.getName(), neo4jSolarSystem.getId(),lastChecked);
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
      var query = generateDefaultQuery(neo4jSolarSystem,start,end);
      influxConnection.getClient().getQueryApi().query(query);
      LOG.info("Updated Day data for System {} from {} to {}", neo4jSolarSystem.getId(),start,end);
    }

    s = s.minusDays(2);
    //cal.add(Calendar.DATE, -3);
    //var time = ZonedDateTime.ofInstant(cal.toInstant(),cal.getTimeZone().toZoneId());
    if(lastChecked == null || neo4jSolarSystem.getLastCalculation() == null || s.isAfter(neo4jSolarSystem.getLastCalculation())){
      neo4jSolarSystemRepository.updateLastCalculation(neo4jSolarSystem.getId(),s);
    }
  }

  @Scheduled(fixedDelayString = "${timing.updateDayData:900000}",initialDelayString = "${timing.delayDayData:0}")//check every 15 minutes
  public void updateDayData(){
    updateDayData(null);
  }

  public void updateDayData(ZonedDateTime before){
    LOG.info("Running updateDayData scheduler (every 15 min)");
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -2);
    calendar.add(Calendar.HOUR, -22);
    // conversion
    //ZonedDateTime now = ZonedDateTime.now(); //for debug purpose
    if(before == null){
      before = ZonedDateTime.ofInstant(calendar.toInstant(),calendar.getTimeZone().toZoneId());
    }
    //ZonedDateTime before = ZonedDateTime.ofInstant(calendar.toInstant(),ZoneId.of("UTC"));

    var list = neo4jSolarSystemRepository.findAllLastCalculationUnset(before);
    for (Neo4jSolarSystem neo4jSolarSystem : list) {
      runInitial(neo4jSolarSystem, neo4jSolarSystem.getLastCalculation());
    }

    list = neo4jSolarSystemRepository.findAllDayCalculationIsMandatory(before);
    for (Neo4jSolarSystem neo4jSolarSystem : list) {
      runInitial(neo4jSolarSystem, neo4jSolarSystem.getLastCalculation());
    }
  }

  //TODO move to own microservice
  public void runUpdateLastDays(Neo4jSolarSystem neo4jSolarSystem,ZonedDateTime day){
    var zId = ZoneId.of(neo4jSolarSystem.getTimezone());
    //Date date = Date.from(instant);

    var s = ZonedDateTime.ofInstant(day.toInstant(),zId).toLocalDate().atStartOfDay(zId);

    if(s.getHour() != 0){
      LOG.error("Error start hour offset not not 0 it is {} instead -> skipped writing to database",s.getHour());
      return;
    }

    var start = zoneFormatter.format(s);
    s = s.plusDays(1);
    var end = zoneFormatter.format(s);
    var query = generateDefaultQuery(neo4jSolarSystem,start,end);
    /*cal.add(Calendar.MILLISECOND, -1);
    end = formatter.format(cal.getTime());
    deleteAllDayData(solarSystem,OffsetDateTime.parse(start),OffsetDateTime.parse(end));
    cal.add(Calendar.MILLISECOND, 1);
    end = formatter.format(cal.getTime());*/
    influxConnection.getClient().getQueryApi().query(query);
    LOG.info("Updated Day data for System {} from {} to {}", neo4jSolarSystem.getId(),start,end);
  }
}
