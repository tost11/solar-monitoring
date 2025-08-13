package de.tostsoft.solarmonitoring.app.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DailyCalculationTest extends AppBaseTest {

    @Autowired
    private InfluxTaskService influxTaskService;

    Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    void checkCalculatesAfterwardsDailyCalculationCorrect() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);

        system = solarSystemRepository.save(system);

        SampleDTO dto1 = new SampleDTO();
        SampleDTO dto2 = new SampleDTO();

        dto1.setDuration(300.f);
        dto2.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();
        var deviceDTO2 = new DeviceDTO();

        deviceDTO1.setId(1L);
        deviceDTO2.setId(2L);

        dto1.setDevices(List.of(deviceDTO1));
        dto2.setDevices(List.of(deviceDTO2));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(300f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(3000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(1000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(2000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(6000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
    }


    @Test
    void checkCalculatesAfterwardsDailyCalculationCorrectDifferentSystems() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");

        system1.setCalculateCombinedValuesAfterwards(true);
        system1 = solarSystemRepository.save(system1);

        system2.setCalculateCombinedValuesAfterwards(true);
        system2 = solarSystemRepository.save(system2);

        SampleDTO dto1 = new SampleDTO();
        SampleDTO dto2 = new SampleDTO();

        dto1.setDuration(300.f);
        dto2.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();
        var deviceDTO2 = new DeviceDTO();

        deviceDTO1.setId(1L);
        deviceDTO2.setId(2L);

        dto1.setDevices(List.of(deviceDTO1));
        dto2.setDevices(List.of(deviceDTO2));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        doRestRequest("api/system/statistics/"+system1.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));
        doRestRequest("api/system/statistics/"+system2.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system1.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));
        LOG.info(statisticDTO.getBody());
        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();
        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(100);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(1000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(1000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);

        statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system2.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));
        LOG.info(statisticDTO.getBody());
        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();
        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(2000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(4000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
    }




    @Test
    void checkDailyCalculationMultipleDevicesCorrect() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        SampleDTO dto1 = new SampleDTO();

        dto1.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();
        var deviceDTO2 = new DeviceDTO();

        deviceDTO1.setId(1L);
        deviceDTO2.setId(2L);

        dto1.setDevices(List.of(deviceDTO1,deviceDTO2));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(3 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(300f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(3000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(1000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(2000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(6000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
    }


    @Test
    void checkDailyCalculationCorrect() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        SampleDTO dto1 = new SampleDTO();

        dto1.setDuration(300.f);

        dto1.setInputTotalKWH(1000f);
        dto1.setOutputTotalKWH(10000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputTotalKWH(900f);
        dto1.setOutputTotalKWH(9000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        dto1.setInputTotalKWH(800f);
        dto1.setOutputTotalKWH(8000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputTotalKWH(600f);
        dto1.setOutputTotalKWH(6000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(3 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(1000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
    }

    @Test
    void checkCalculatedValuesForDailyCalculationCorrect() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        SampleDTO dto1 = new SampleDTO();

        dto1.setDuration(36f);

        dto1.setInputWatt(1000000f);
        dto1.setOutputWatt(10000000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        dto1.setInputWatt(2000000f);
        dto1.setOutputWatt(20000000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(3 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(10);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(100);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(20);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(200);
    }

    @Test
    void checkCalculatesAfterwardsDailyCalculationCalculationAndSetAreCombined() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);

        system = solarSystemRepository.save(system);

        SampleDTO dto1 = new SampleDTO();
        SampleDTO dto2 = new SampleDTO();

        dto1.setDuration(300.f);
        dto2.setDuration(36f);

        var deviceDTO1 = new DeviceDTO();
        var deviceDTO2 = new DeviceDTO();

        deviceDTO1.setId(1L);
        deviceDTO2.setId(2L);

        dto1.setDevices(List.of(deviceDTO1));
        dto2.setDevices(List.of(deviceDTO2));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(1000000f);
        deviceDTO2.setOutputWatt(10000000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(2000000f);
        deviceDTO2.setOutputWatt(20000000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(110);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsInt()).isEqualTo(10);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(1100);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(1000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsInt()).isEqualTo(100);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(220);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsInt()).isEqualTo(20);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(2200);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsInt()).isEqualTo(200);
    }

}