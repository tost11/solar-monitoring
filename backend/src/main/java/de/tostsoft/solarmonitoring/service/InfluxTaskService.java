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

  final String yesterdayStartTime = "experimental.addDuration(d: -1d, to: today())";
  final String todayStartTime = "today()";

  DateFormat formatter = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

  private String generateSumQuery(long systemId,InfluxMeasurement influxMeasurement,long userId,String sourceMeasurement,String targetMeasurement,String start,String end){
    return "from(bucket: \"user-"+userId+"\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\" or r[\"_field\"] == \"Duration\")\n"
      + "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
      + "  |> map(fn: (r) => ({r with _value: r."+sourceMeasurement+" * r.Duration / 1000. / 60.}))\n"
      + "  |> cumulativeSum()\n"
      + "  |> max()\n"
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \"day-values\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"user-" + userId + "\")\n\n";
  }

  private String generateTotalSumQuery(long systemId,InfluxMeasurement influxMeasurement,long userId,String sourceMeasurement,String targetMeasurement,String start,String end){
    return "from(bucket: \"user-"+userId+"\")\n"
      + "  |> range(start: "+start+", stop: "+end+")\n"
      + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+influxMeasurement+"\")\n"
      + "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n"
      + "  |> filter(fn: (r) => r[\"_field\"] == \""+sourceMeasurement+"\")\n"
      + "  |> spread()\n"
      + "  |> map(fn: (r) => ({r with _time: "+start+",_measurement: \"day-values\",_field:\""+targetMeasurement+"\"}))\n"
      + "  |> to(bucket: \"user-"+userId+"\")\n\n";
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
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalProductionKWH",prodKWHField,start,end);
    }
    if(InfluxController.SIMPLE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SIMPLE,solarSystem.getRelationOwnedBy().getId(),"TotalProductionKWH",prodKWHField,start,end);
    }
    if(InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.GRID,solarSystem.getRelationOwnedBy().getId(),"TotalKWH",prodKWHField,start,end);
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
      return generateTotalSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalConsumptionKWH",consKWHField,start,end);
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

  public void runInitial(SolarSystem solarSystem){
    deleteAllDayData(solarSystem);
    runInitial(solarSystem,null);
  }

  private void runInitial(SolarSystem solarSystem,LocalDateTime lastChecked){

    if(lastChecked == null) {
      LOG.info("Running full day generation for system {} with id {}", solarSystem.getName(), solarSystem.getId());
    }else{
      LOG.info("Running day generation for system {} with id {} from {}", solarSystem.getName(), solarSystem.getId(),lastChecked);
    }

    var formatter = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

    var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
    //Date date = Date.from(instant);

    var s = solarSystem.getCreationDate().toLocalDate().atStartOfDay(zId);

    if(lastChecked != null){
      s = lastChecked.toLocalDate().atStartOfDay(zId);
    }

    Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    while(true){
      var start = formatter.format(cal.getTime());
      cal.add(Calendar.DATE, 1);
      if(cal.getTimeInMillis() > new Date().getTime()){
        break;
      }
      var end = formatter.format(cal.getTime());
      var query = generateDefaultQuery(solarSystem,start,end);
      influxConnection.getClient().getQueryApi().query(query);
    }

    cal.add(Calendar.DATE, -2);
    LocalDateTime localDateTime = LocalDateTime.ofInstant(cal.toInstant(), zId);
    if(localDateTime.isAfter(solarSystem.getLastCalculation())){
      solarSystemRepository.updateLastCalculation(solarSystem.getId(),localDateTime);
    }
  }

  @Scheduled(fixedDelay = 1000 * 60 * 15)//check every 15 minutes
  public void updateDayData(){
    LOG.info("Running updateDayData scheduler (every 15 min)");
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -1);
    calendar.add(Calendar.HOUR, -22);
    // conversion
    LocalDateTime localDateTime = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC"));
    var list = solarSystemRepository.findAllDayCalculationIsMandatory(localDateTime);
    for (SolarSystem solarSystem : list) {
      runInitial(solarSystem,localDateTime);
    }
  }

  //TODO move to own microservice
  public void runUpdateLastDays(SolarSystem solarSystem,LocalDateTime day){
    var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
    //Date date = Date.from(instant);

    var s = day.toLocalDate().atStartOfDay(zId);

    Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    var start = formatter.format(cal.getTime());
    cal.add(Calendar.DATE, 1);
    var end = formatter.format(cal.getTime());
    var query = generateDefaultQuery(solarSystem,start,end);
    influxConnection.getClient().getQueryApi().query(query);
  }
}
