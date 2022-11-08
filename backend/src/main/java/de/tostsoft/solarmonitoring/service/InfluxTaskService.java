package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.controller.InfluxController;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InfluxTaskService {

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  @Autowired
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private InfluxConnection influxConnection;

  public static final String calcProdKWHField = "CalcProducedKWH";
  public static final String calcConsKWHField = "CalcConsumedKWH";

  public static final String prodKWHField = "ProducedKWH";
  public static final String consKWHField = "ConsumedKWH";

  public static final String prodKWHFieldSum = "ProducedKWH_sum";
  public static final String consKWHFieldSum = "ConsumedKWH_sum";

  final String yesterdayStartTime = "experimental.addDuration(d: -1d, to: today())";
  final String todayStartTime = "today()";

  private String generateSumQuery(long systemId,InfluxMeasurement influxMeasurement,long userId,String sourceMeasurement,String targetMeasurement,String start,String end){
    return "from(bucket: \"user-"+userId+"\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\" or r[\"_field\"] == \"Duration\")\n"
      + "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
      + "  |> map(fn: (r) => ({r with _value: r."+sourceMeasurement+" * r.Duration / 3600000.}))\n"
      + "  |> cumulativeSum()\n"
      + "  |> max()\n"
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \"day-values\",_field:\""+targetMeasurement+"\"}))\n"
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
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \"day-values\",_field:\""+targetMeasurement+"\"}))\n"
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
        + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \"day-values\",_field:\""+targetMeasurement+"_sum\"}))\n"
        + "  |> to(bucket: \"user-"+userId+"\")\n\n";
    }
    return q;
  }

  private String generateProductionQuery(SolarSystem solarSystem,String start,String end){
    if(InfluxController.SELFMADE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",calcProdKWHField,start,end);
    }
    if(InfluxController.SIMPLE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SIMPLE,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",calcProdKWHField,start,end);
    }
    if(InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.GRID,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",calcProdKWHField,start,end);
    }
    return "";
  }

  private String generateTotalProductionQuery(SolarSystem solarSystem,String start,String end){
    if(InfluxController.SELFMADE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalProductionKWH",prodKWHField,start,end,false);
    }
    if(InfluxController.SIMPLE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SIMPLE,solarSystem.getRelationOwnedBy().getId(),"TotalProductionKWH",prodKWHField,start,end,false);
    }
    if(InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.GRID,solarSystem.getRelationOwnedBy().getId(),"TotalKWH",prodKWHField,start,end,true);
    }
    return "";
  }

  private String generateConsumptionQuery(SolarSystem solarSystem,String start,String end){
    if(solarSystem.getType() == SolarSystemType.SELFMADE_CONSUMPTION ||
        solarSystem.getType() == SolarSystemType.SELFMADE_INVERTER ||
        solarSystem.getType() == SolarSystemType.SELFMADE_DEVICE){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalConsumption",calcConsKWHField,start,end);
    }
    return "";
  }

  private String generateTotalConsumptionQuery(SolarSystem solarSystem,String start,String end){
    if(solarSystem.getType() == SolarSystemType.SELFMADE_CONSUMPTION ||
            solarSystem.getType() == SolarSystemType.SELFMADE_INVERTER ||
            solarSystem.getType() == SolarSystemType.SELFMADE_DEVICE){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalConsumptionKWH",consKWHField,start,end,false);
    }
    return "";
  }

  String generateDefaultQuery(SolarSystem solarSystem){
    //return "import \"experimental\"\n\noption task = {}\n" +
    return "" +
      generateConsumptionQuery(solarSystem,yesterdayStartTime,todayStartTime) +
      generateProductionQuery(solarSystem,yesterdayStartTime,todayStartTime) +
      generateTotalConsumptionQuery(solarSystem,yesterdayStartTime,todayStartTime) +
      generateTotalProductionQuery(solarSystem,yesterdayStartTime,todayStartTime);
  }

  String generateDefaultQuery(SolarSystem solarSystem,String start, String end){
    return "" +
      generateConsumptionQuery(solarSystem,start,end) +
      generateProductionQuery(solarSystem,start,end) +
      generateTotalConsumptionQuery(solarSystem,start,end) +
      generateTotalProductionQuery(solarSystem,start,end);
  }

  public void runAllInitialTasks(){
    int pageSize = 100;
    int offset = 0;

    var systems = solarSystemRepository.getPage(pageSize,offset);
    while(!systems.isEmpty()){
      for (SolarSystem system : systems) {
        runInitial(system);
      }
      offset+=pageSize;
      systems = solarSystemRepository.getPage(pageSize,offset);
    }
  }

  public void deleteAllDayData(SolarSystem solarSystem){
    influxConnection.getClient().getDeleteApi().delete(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),OffsetDateTime.now(),"_measurement=\"day-values\" AND system=\""+solarSystem.getId()+"\"","user-"+solarSystem.getRelationOwnedBy().getId(),"my-org");
  }

  public void deleteAllDayData(SolarSystem solarSystem,OffsetDateTime from,OffsetDateTime to){
    influxConnection.getClient().getDeleteApi().delete(from,to,"_measurement=\"day-values\" AND system=\""+solarSystem.getId()+"\"","user-"+solarSystem.getRelationOwnedBy().getId(),"my-org");
  }

  public void runInitial(SolarSystem solarSystem){
    deleteAllDayData(solarSystem);
    runInitial(solarSystem,null);
  }

  private void runInitial(SolarSystem solarSystem,ZonedDateTime lastChecked){

    if(lastChecked == null) {
      LOG.info("Running full day generation for system {} with id {}", solarSystem.getName(), solarSystem.getId());
    }else{
      LOG.info("Running day generation for system {} with id {} from {}", solarSystem.getName(), solarSystem.getId(),lastChecked);
    }

    var formatter = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

    var zId = ZoneId.of(solarSystem.getTimezone());
    //Date date = Date.from(instant);
    formatter.setTimeZone(TimeZone.getTimeZone(zId));

    var s = ZonedDateTime.ofInstant(solarSystem.getCreationDate().toInstant(),zId).toLocalDate().atStartOfDay(zId);

    //var s = solarSystem.getCreationDate().toLocalDate().atStartOfDay(zId);

    if(lastChecked != null){
      s = ZonedDateTime.ofInstant(lastChecked.toInstant(),zId).toLocalDate().atStartOfDay(zId);
    }

    Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zId));
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    if(lastChecked != null){
      cal.add(Calendar.DATE, 1);
    }

    if(cal.get(Calendar.HOUR_OF_DAY) != 0){
      System.out.println(cal.get(Calendar.HOUR_OF_DAY));
      LOG.error("Error start hour offset not not 0 it is {} instead -> skipped writing to database",cal.get(Calendar.HOUR_OF_DAY));
      return;
    }

    while(true){
      var start = formatter.format(cal.getTime());
      cal.add(Calendar.DATE, 1);
      if(cal.getTimeInMillis() > new Date().getTime()){
        break;
      }
      var end = formatter.format(cal.getTime());
      var query = generateDefaultQuery(solarSystem,start,end);
      influxConnection.getClient().getQueryApi().query(query);
      LOG.info("Updated Day data for System {} from {} to {}",solarSystem.getId(),start,end);
    }

    cal.add(Calendar.DATE, -1);
    //var time = ZonedDateTime.ofInstant(cal.toInstant(),cal.getTimeZone().toZoneId());
    var time = ZonedDateTime.ofInstant(cal.toInstant(),zId);
    if(lastChecked == null || solarSystem.getLastCalculation() == null || time.isAfter(solarSystem.getLastCalculation())){
      solarSystemRepository.updateLastCalculation(solarSystem.getId(),time);
    }
  }

  @Scheduled(fixedDelay = 1000 * 60 * 15)//check every 15 minutes
  //@Scheduled(fixedDelay = 1000 * 30)//for debugging
  public void updateDayData(){
    LOG.info("Running updateDayData scheduler (every 15 min)");
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -2);
    calendar.add(Calendar.HOUR, -22);
    // conversion
    //ZonedDateTime now = ZonedDateTime.now(); //for debug purpose
    ZonedDateTime before = ZonedDateTime.ofInstant(calendar.toInstant(),calendar.getTimeZone().toZoneId());
    //ZonedDateTime before = ZonedDateTime.ofInstant(calendar.toInstant(),ZoneId.of("UTC"));

    var list = solarSystemRepository.findAllLastCalculationUnset(before);
    for (SolarSystem solarSystem : list) {
      runInitial(solarSystem,solarSystem.getLastCalculation());
    }

    list = solarSystemRepository.findAllDayCalculationIsMandatory(before);
    for (SolarSystem solarSystem : list) {
      runInitial(solarSystem,solarSystem.getLastCalculation());
    }
  }

  //TODO move to own microservice
  public void runUpdateLastDays(SolarSystem solarSystem,ZonedDateTime day){
    var zId = ZoneId.of(solarSystem.getTimezone());
    //Date date = Date.from(instant);

    DateFormat formatter = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
    formatter.setTimeZone(TimeZone.getTimeZone(zId));

    var s = ZonedDateTime.ofInstant(day.toInstant(),zId).toLocalDate().atStartOfDay(zId);

    Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zId));
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    if(cal.get(Calendar.HOUR_OF_DAY) != 0){
      System.out.println(cal.get(Calendar.HOUR_OF_DAY));
      LOG.error("Error start hour offset not not 0 it is {} instead -> skipped writing to database",cal.get(Calendar.HOUR_OF_DAY));
      return;
    }

    var start = formatter.format(cal.getTime());
    cal.add(Calendar.DATE, 1);
    var end = formatter.format(cal.getTime());
    var query = generateDefaultQuery(solarSystem,start,end);
    cal.add(Calendar.MILLISECOND, -1);
    end = formatter.format(cal.getTime());
    deleteAllDayData(solarSystem,OffsetDateTime.parse(start),OffsetDateTime.parse(end));
    cal.add(Calendar.MILLISECOND, 1);
    end = formatter.format(cal.getTime());
    influxConnection.getClient().getQueryApi().query(query);
    LOG.info("Updated Day data for System {} from {} to {}",solarSystem.getId(),start,end);
  }
}
