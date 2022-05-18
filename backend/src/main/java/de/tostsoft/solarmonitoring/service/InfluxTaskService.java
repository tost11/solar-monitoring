package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.TasksQuery;
import com.influxdb.client.domain.Task;
import com.influxdb.client.domain.TaskCreateRequest;
import com.influxdb.client.domain.TaskStatusType;
import com.influxdb.client.domain.TaskUpdateRequest;
import de.tostsoft.solarmonitoring.controller.InfluxController;
import de.tostsoft.solarmonitoring.controller.data.SelfmadeSolarController;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

  final String prodKWHField = "calcProducedKWH";
  final String consKWHField = "calcConsumedKWH";

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
        + "  |> to(bucket: \"user-"+userId+"\")\n\n";
  }

  private String generateProductionQuery(SolarSystem solarSystem,String start,String end){
    if(InfluxController.SELFMADE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",prodKWHField,start,end);
    }
    if(InfluxController.SIMPLE_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SIMPLE,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",prodKWHField,start,end);
    }
    if(InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString())){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.GRID,solarSystem.getRelationOwnedBy().getId(),"ChargeWatt",prodKWHField,start,end);
    }
    return "";
  }

  private String generateConsumptionQuery(SolarSystem solarSystem,String start,String end){
    if(solarSystem.getType() == SolarSystemType.SELFMADE_CONSUMPTION ||
        solarSystem.getType() == SolarSystemType.SELFMADE_INVERTER ||
        solarSystem.getType() == SolarSystemType.SELFMADE_DEVICE){
      return generateSumQuery(solarSystem.getId(),InfluxMeasurement.SELFMADE,solarSystem.getRelationOwnedBy().getId(),"TotalConsumption",consKWHField,start,end);
    }
    return "";
  }

  public String generateSolarTaskName(SolarSystem solarSystem){
    return "user-"+solarSystem.getRelationOwnedBy().getId()+"-system-"+solarSystem.getId()+"_day-values";
  }

  String generateDefaultQuery(SolarSystem solarSystem){
    //return "import \"experimental\"\n\noption task = {}\n" +
    return "" +
        generateConsumptionQuery(solarSystem,yesterdayStartTime,todayStartTime) +
        generateProductionQuery(solarSystem,yesterdayStartTime,todayStartTime);
  }

  String generateDefaultQuery(SolarSystem solarSystem,String start, String end){
    return generateConsumptionQuery(solarSystem,start,end) + generateProductionQuery(solarSystem,start,end);
  }

  String generateTaskHeader(String name){
    return "import \"experimental\"\n"
        + "\n"
        + "option task = {name: \""+name+"\", cron: \"12 0 * * * *\"}"
        + "\n";
  }

  //TODO handle timezone
  public void updateSystemTask(SolarSystem solarSystem){
    var taskName = generateSolarTaskName(solarSystem);
    LOG.info("Try to update task with name: {}",taskName);

    var client = influxConnection.getClient();

    var taskQuery = new TasksQuery();
    taskQuery.setName(taskName);

    Task task = null;
    var tasks = client.getTasksApi().findTasks(taskQuery);
    if(!tasks.isEmpty()){
      task = tasks.get(0);
    }

    if(task == null){
      if(solarSystem.getLabels().contains(Neo4jLabels.IS_DELETED.toString())) {
        return;
      }
      LOG.info("Creating new system task");

      TaskCreateRequest taskCreateRequest = new TaskCreateRequest()
          .orgID(influxConnection.getOrganizaionId())
          .flux(generateTaskHeader(taskName)+generateDefaultQuery(solarSystem))
          .status(TaskStatusType.ACTIVE);
      client.getTasksApi().createTask(taskCreateRequest);
    }else {
      if (solarSystem.getLabels().contains(Neo4jLabels.IS_DELETED.toString())) {
        LOG.info("Removing old System task");
        client.getTasksApi().deleteTask(task);
        return;
      }
      LOG.info("Updating System task");
      var t = new TaskUpdateRequest();
      t.setFlux(generateTaskHeader(taskName) + generateDefaultQuery(solarSystem));
      client.getTasksApi().updateTask (task.getId(),t);
    }
  }

  public void updateAllTasks(){
    int pageSize = 100;
    int offset = 0;

    var systems = solarSystemRepository.getPage(pageSize,offset);
    while(!systems.isEmpty()){
      for (SolarSystem system : systems) {
        updateSystemTask(system);
      }
      offset+=pageSize;
      systems = solarSystemRepository.getPage(pageSize,offset);
    }
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

  public void runInitial(SolarSystem solarSystem){

    LOG.info("Running full influx day generation for system {} with id {}",solarSystem.getName(),solarSystem.getId());

    var formatter = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

    var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
    //Date date = Date.from(instant);

    var s = solarSystem.getCreationDate().toLocalDate().atStartOfDay(zId);
    Date d = Date.from(s.toInstant());
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    influxConnection.getClient().getDeleteApi().delete(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),OffsetDateTime.now(),"_measurement=\"day-values\" AND system=\""+solarSystem.getId()+"\"","user-"+solarSystem.getRelationOwnedBy().getId(),"my-org");

    while(true){
      var start = formatter.format(cal.getTime());
      cal.add(Calendar.DATE, 1);
      var end = formatter.format(cal.getTime());
      var query = generateDefaultQuery(solarSystem,start,end);
      influxConnection.getClient().getQueryApi().query(query);
      if(cal.getTimeInMillis() > new Date().getTime()){
        break;
      }
    }
  }
}
