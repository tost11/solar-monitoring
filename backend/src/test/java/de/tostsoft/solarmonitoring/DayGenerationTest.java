package de.tostsoft.solarmonitoring;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.InfluxService;
import de.tostsoft.solarmonitoring.service.InfluxTaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.Assertions.*;

//only for manual use because enviorment is neeeded
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("debug")
public class DayGenerationTest {

  private static final Logger LOG = LoggerFactory.getLogger(SolarControllerTest.class);

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private InfluxConnection influxConnection;

  @Autowired
  private InfluxService influxService;

  @Value("${debug.token:}")
  private String debugToken;
  @Value("${debug.username}")
  private String username;
  @Value("${debug.password}")
  private String password;
  @Value("${debug.system}")
  private String system;

  @LocalServerPort
  private int randomServerPort;

  @Autowired
  private DebugService debugService;

  @Autowired
  private InfluxTaskService influxTaskService;

  private void cleanUpData() {

    LOG.info("Delete Influx buckets");
    for (Bucket bucket : influxConnection.getBuckets()) {
      if(bucket.getName().startsWith("_")){//skip system buckets
        continue;
      }
      influxConnection.deleteBucket(bucket.getName());
    }
    solarSystemRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  private void initDatabaseStuff(){
    cleanUpData();
    debugService.crateTestUserWithSystem(SolarSystemType.SELFMADE);
  }

  long getTimeStampAt(ZonedDateTime today,int daysTuSub,int hour,int minute){
    var s = today.minusDays(daysTuSub).withHour(hour).withMinute(minute);
    Instant instant = s.toInstant();
    return instant.toEpochMilli();
  }

  void addSamples(SampleDTO sampleDTO,RestTemplate restTemplate,HttpHeaders headers, SolarSystem system,ZonedDateTime zonedDateTime,float mult,int daysToSub){
    sampleDTO.setInputWatt(1000f * mult);
    sampleDTO.setOutputWatt(1000f * mult);

    long d = getTimeStampAt(zonedDateTime , daysToSub,0,1 ) ;
    sampleDTO.setTimestamp(d);
    var entity = new HttpEntity<>(sampleDTO, headers);
    restTemplate.postForEntity("http://localhost:"+randomServerPort+"/api/solar/data?systemId="+system.getId(),entity,String.class);

    d = getTimeStampAt( zonedDateTime,daysToSub,23,59 ) ;
    sampleDTO.setTimestamp(d);
    entity = new HttpEntity<>(sampleDTO, headers);
    restTemplate.postForEntity("http://localhost:"+randomServerPort+"/api/solar/data?systemId="+system.getId(),entity,String.class);
  }


  private static List<Arguments> getAllTimeZones() {

    var res = new ArrayList<Arguments>();

    for(int i = -12; i<=12;i++){
      res.add(Arguments.of(ZoneId.of ("Etc/GMT"+(i<0?"":"+")+i)));
    }

    return res;
  }

  private void validateNumDayValues(SolarSystem system,int num){

    var query = "from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n"
        + "  |> range(start: 0, stop: now())\n"
        + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+ InfluxMeasurement.SOLAR_DAY_DATA+ "\")";

    var res = influxConnection.getClient().getQueryApi().query(query);

    assertThat(res.get(0).getRecords()).hasSize(num);
  }

  @ParameterizedTest
  @MethodSource("getAllTimeZones")
  public void testLastDayCalculation(ZoneId zoneToTest){

    var simpleDate = new Date();
    ZonedDateTime today = ZonedDateTime.now(zoneToTest);

    var system = solarSystemRepository.findAll().get(0);
    system.setTimezone(zoneToTest.getId());
    system = solarSystemRepository.save(system);

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.set("clientToken",debugToken);

    SampleDTO sampleDTO = SampleDTO.builder()
        .duration(10f*6*60)
        .build();

    addSamples(sampleDTO,restTemplate,headers,system,today,1,5);
    addSamples(sampleDTO,restTemplate,headers,system,today,2,4);

    system = solarSystemRepository.save(system);
    addSamples(sampleDTO,restTemplate,headers,system,today,3,3);

    system = solarSystemRepository.save(system);
    addSamples(sampleDTO,restTemplate,headers,system,today,4,2);

    system = solarSystemRepository.save(system);
    addSamples(sampleDTO,restTemplate,headers,system,today,5,1);

    system = solarSystemRepository.save(system);
    addSamples(sampleDTO,restTemplate,headers,system,today,6,0);

    influxTaskService.runInitial(system);

    validateNumDayValues(system,4);

    influxTaskService.deleteAllDayData(system);

    system.setLastCalculation(today.minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0));
    system = solarSystemRepository.save(system);

    influxTaskService.updateDayData();
    validateNumDayValues(system,1);

    influxTaskService.deleteAllDayData(system);

    Calendar c = Calendar.getInstance();
    c.setTime(simpleDate);
    c.add(Calendar.DATE,-7);
    influxService.getStatisticsDataAsJson(system.getRelationOwnedBy().getId(),system.getId(),c.getTime(),simpleDate,false);

    validateNumDayValues(system,2);
  }
}
