package de.tostsoft.solarmonitoring.app.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DailyCalculationTest extends AppBaseTest {

    @Autowired
    private InfluxTaskService influxTaskService;

    Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private InfluxService influxService;

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
        deviceDTO1.setBatteryTotalKWH(100f);
        deviceDTO1.setGridTotalConsumptionKWH(10f);
        deviceDTO1.setGridTotalFeedInKWH(100000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        deviceDTO2.setBatteryTotalKWH(200f);
        deviceDTO2.setGridTotalConsumptionKWH(20f);
        deviceDTO2.setGridTotalFeedInKWH(200000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO1.setBatteryTotalKWH(90f);
        deviceDTO1.setGridTotalConsumptionKWH(9f);
        deviceDTO1.setGridTotalFeedInKWH(90000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        deviceDTO2.setBatteryTotalKWH(180f);
        deviceDTO2.setGridTotalConsumptionKWH(18f);
        deviceDTO2.setGridTotalFeedInKWH(180000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO1.setBatteryTotalKWH(80f);
        deviceDTO1.setGridTotalConsumptionKWH(8f);
        deviceDTO1.setGridTotalFeedInKWH(80000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        deviceDTO2.setBatteryTotalKWH(160f);
        deviceDTO2.setGridTotalConsumptionKWH(16f);
        deviceDTO2.setGridTotalFeedInKWH(160000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO1.setBatteryTotalKWH(60f);
        deviceDTO1.setGridTotalConsumptionKWH(6f);
        deviceDTO1.setGridTotalFeedInKWH(60000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        deviceDTO2.setBatteryTotalKWH(120f);
        deviceDTO2.setGridTotalConsumptionKWH(12f);
        deviceDTO2.setGridTotalFeedInKWH(120000f);
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
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(30f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(3f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(30000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(10000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(20000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(6000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(60f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(40f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(6f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(4f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(60000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(20000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(40000f);
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
        deviceDTO1.setBatteryTotalKWH(100f);
        deviceDTO1.setGridTotalConsumptionKWH(10f);
        deviceDTO1.setGridTotalFeedInKWH(100000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        deviceDTO2.setBatteryTotalKWH(200f);
        deviceDTO2.setGridTotalConsumptionKWH(20f);
        deviceDTO2.setGridTotalFeedInKWH(200000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO1.setBatteryTotalKWH(90f);
        deviceDTO1.setGridTotalConsumptionKWH(9f);
        deviceDTO1.setGridTotalFeedInKWH(90000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        deviceDTO2.setBatteryTotalKWH(180f);
        deviceDTO2.setGridTotalConsumptionKWH(18f);
        deviceDTO2.setGridTotalFeedInKWH(180000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(1)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO1.setBatteryTotalKWH(80f);
        deviceDTO1.setGridTotalConsumptionKWH(8f);
        deviceDTO1.setGridTotalFeedInKWH(80000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        deviceDTO2.setBatteryTotalKWH(160f);
        deviceDTO2.setGridTotalConsumptionKWH(16f);
        deviceDTO2.setGridTotalFeedInKWH(160000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO1.setBatteryTotalKWH(60f);
        deviceDTO1.setGridTotalConsumptionKWH(6f);
        deviceDTO1.setGridTotalFeedInKWH(60000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        deviceDTO2.setBatteryTotalKWH(120f);
        deviceDTO2.setGridTotalConsumptionKWH(12f);
        deviceDTO2.setGridTotalFeedInKWH(120000f);
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
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(10000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(10000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(20000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(20000f);

        statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system2.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));
        LOG.info(statisticDTO.getBody());
        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();
        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(20000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(20000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(4000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(40f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(40f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(4);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(4);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(40000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(40000f);
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
        deviceDTO1.setBatteryTotalKWH(100f);
        deviceDTO1.setGridTotalConsumptionKWH(10f);
        deviceDTO1.setGridTotalFeedInKWH(100000f);
        deviceDTO2.setInputTotalKWH(2000f);
        deviceDTO2.setOutputTotalKWH(20000f);
        deviceDTO2.setBatteryTotalKWH(200f);
        deviceDTO2.setGridTotalConsumptionKWH(20f);
        deviceDTO2.setGridTotalFeedInKWH(20f);
        deviceDTO2.setGridTotalFeedInKWH(200000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO1.setBatteryTotalKWH(90f);
        deviceDTO1.setGridTotalConsumptionKWH(9f);
        deviceDTO1.setGridTotalFeedInKWH(90000f);
        deviceDTO2.setInputTotalKWH(1800f);
        deviceDTO2.setOutputTotalKWH(18000f);
        deviceDTO2.setBatteryTotalKWH(180f);
        deviceDTO2.setGridTotalConsumptionKWH(18f);
        deviceDTO2.setGridTotalFeedInKWH(180000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO1.setBatteryTotalKWH(80f);
        deviceDTO1.setGridTotalConsumptionKWH(8f);
        deviceDTO1.setGridTotalFeedInKWH(80000f);
        deviceDTO2.setInputTotalKWH(1600f);
        deviceDTO2.setOutputTotalKWH(16000f);
        deviceDTO2.setBatteryTotalKWH(160f);
        deviceDTO2.setGridTotalConsumptionKWH(16f);
        deviceDTO2.setGridTotalFeedInKWH(160000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO1.setBatteryTotalKWH(60f);
        deviceDTO1.setGridTotalConsumptionKWH(6f);
        deviceDTO1.setGridTotalFeedInKWH(60000f);
        deviceDTO2.setInputTotalKWH(1200f);
        deviceDTO2.setOutputTotalKWH(12000f);
        deviceDTO2.setBatteryTotalKWH(120f);
        deviceDTO2.setGridTotalConsumptionKWH(12f);
        deviceDTO2.setGridTotalFeedInKWH(120000f);
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
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(30f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(3f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(30000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(10000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(20000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(6000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsFloat()).isEqualTo(4000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(60f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-2").getAsFloat()).isEqualTo(40f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(6f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-2").getAsFloat()).isEqualTo(4f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(60000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(20000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-2").getAsFloat()).isEqualTo(40000f);
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
        dto1.setBatteryTotalKWH(100f);
        dto1.setGridTotalConsumedKWH(10f);
        dto1.setGridTotalFeedInKWH(100000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputTotalKWH(900f);
        dto1.setOutputTotalKWH(9000f);
        dto1.setBatteryTotalKWH(90f);
        dto1.setGridTotalConsumedKWH(9f);
        dto1.setGridTotalFeedInKWH(90000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        dto1.setInputTotalKWH(800f);
        dto1.setOutputTotalKWH(8000f);
        dto1.setBatteryTotalKWH(80f);
        dto1.setGridTotalConsumedKWH(8f);
        dto1.setGridTotalFeedInKWH(80000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputTotalKWH(600f);
        dto1.setOutputTotalKWH(6000f);
        dto1.setBatteryTotalKWH(60f);
        dto1.setGridTotalConsumedKWH(6f);
        dto1.setGridTotalFeedInKWH(60000f);
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
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(10000f);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsFloat()).isEqualTo(2);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsFloat()).isEqualTo(20000f);
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
        dto1.setBatteryWatt(100000f);
        dto1.setGridWatt(100000000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputWatt(0f);
        dto1.setOutputWatt(0f);
        dto1.setBatteryWatt(0f);
        dto1.setGridWatt(-1000000000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofMinutes(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        dto1.setInputWatt(2000000f);
        dto1.setOutputWatt(20000000f);
        dto1.setBatteryWatt(200000f);
        dto1.setGridWatt(200000000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        dto1.setInputWatt(0f);
        dto1.setOutputWatt(0f);
        dto1.setBatteryWatt(0f);
        dto1.setGridWatt(-2000000000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofMinutes(1)).toEpochMilli());
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
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsInt()).isEqualTo(1);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsInt()).isEqualTo(1000);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsInt()).isEqualTo(10000);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(20);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(200);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsInt()).isEqualTo(2);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsInt()).isEqualTo(2000);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsInt()).isEqualTo(20000);
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

        dto1.setDuration(36f);
        dto2.setDuration(36f);

        var deviceDTO1 = new DeviceDTO();
        var deviceDTO2 = new DeviceDTO();

        deviceDTO1.setId(1L);
        deviceDTO2.setId(2L);

        dto1.setDevices(List.of(deviceDTO1));
        dto2.setDevices(List.of(deviceDTO2));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        deviceDTO1.setBatteryTotalKWH(100f);
        deviceDTO1.setGridTotalConsumptionKWH(1000000f);
        deviceDTO1.setGridTotalFeedInKWH(100000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(1000000f);
        deviceDTO2.setOutputWatt(10000000f);
        deviceDTO2.setBatteryWatt(100000f);
        deviceDTO2.setGridWatt(1000000000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(0f);
        deviceDTO2.setOutputWatt(0f);
        deviceDTO2.setBatteryWatt(0f);
        deviceDTO2.setGridWatt(-100000000f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).plus(Duration.ofMinutes(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO1.setBatteryTotalKWH(90f);
        deviceDTO1.setGridTotalConsumptionKWH(900000f);
        deviceDTO1.setGridTotalFeedInKWH(90000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO1.setBatteryTotalKWH(80f);
        deviceDTO1.setGridTotalConsumptionKWH(800000f);
        deviceDTO1.setGridTotalFeedInKWH(80000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(0f);
        deviceDTO2.setOutputWatt(0f);
        deviceDTO2.setBatteryWatt(0f);
        deviceDTO2.setGridWatt(-200000000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).plus(Duration.ofMinutes(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputWatt(2000000f);
        deviceDTO2.setOutputWatt(20000000f);
        deviceDTO2.setBatteryWatt(200000f);
        deviceDTO2.setGridWatt(2000000000f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).plus(Duration.ofSeconds(5)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO1.setBatteryTotalKWH(60f);
        deviceDTO1.setGridTotalConsumptionKWH(600000f);
        deviceDTO1.setGridTotalFeedInKWH(60000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(15 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsInt()).isEqualTo(10);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(110);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(1000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2").getAsInt()).isEqualTo(100);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(1100);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery-d-2").getAsInt()).isEqualTo(1);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Battery").getAsInt()).isEqualTo(11);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(100000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed-d-2").getAsInt()).isEqualTo(10000);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridConsumed").getAsInt()).isEqualTo(110000);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn").getAsInt()).isEqualTo(11000);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(10000f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("GridFeedIn-d-2").getAsInt()).isEqualTo(1000);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsInt()).isEqualTo(20);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsInt()).isEqualTo(220);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1").getAsFloat()).isEqualTo(2000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2").getAsInt()).isEqualTo(200);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed").getAsInt()).isEqualTo(2200);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-1").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery-d-2").getAsInt()).isEqualTo(2);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Battery").getAsInt()).isEqualTo(22);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-1").getAsFloat()).isEqualTo(200000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed-d-2").getAsInt()).isEqualTo(20000);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridConsumed").getAsInt()).isEqualTo(220000);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn").getAsInt()).isEqualTo(22000);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-1").getAsFloat()).isEqualTo(20000f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("GridFeedIn-d-2").getAsInt()).isEqualTo(2000);
    }

    @Test
    void checkCombinedStatisticResult() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setElectricityPrice(1.f);

        system = solarSystemRepository.save(system);

        influxService.updatePrice(system, ZonedDateTime.ofInstant(Instant.now(),ZoneId.of("UTC")));

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

        statisticDTO = doRestRequest("api/influx/statistics/latest?systemId="+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

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

        //now check only production without token

        statisticDTO = doRestRequest("api/influx/statistics/all?systemId="+system.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET);

        LOG.info(statisticDTO.getBody());

        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(300f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2")).isEqualTo(null);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2")).isEqualTo(null);

        statisticDTO = doRestRequest("api/influx/statistics/latest?systemId="+system.getId(),"", HttpMethod.GET);

        LOG.info(statisticDTO.getBody());

        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(300f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(100f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-1")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Consumed-d-2")).isEqualTo(null);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.minus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced").getAsFloat()).isEqualTo(600f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-1").getAsFloat()).isEqualTo(200f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced-d-2").getAsFloat()).isEqualTo(400f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-1")).isEqualTo(null);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Consumed-d-2")).isEqualTo(null);
    }

    @Test
    void checkTotalCalculationWorking() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setElectricityPrice(0.1f);
        system.setElectricityPriceFeedIn(10f);
        system = solarSystemRepository.save(system);

        influxService.updatePrice(system, ZonedDateTime.ofInstant(Instant.now().minus(Duration.ofDays(2)),ZoneId.of("UTC")));
        influxService.updatePriceFeedIn(system, ZonedDateTime.ofInstant(Instant.now().minus(Duration.ofDays(2)),ZoneId.of("UTC")));

        SampleDTO totalSample = new SampleDTO();
        totalSample.setDuration(30.f);
        totalSample.setInputTotalKWH(10.f);
        totalSample.setOutputTotalKWH(20.f);
        totalSample.setGridTotalConsumedKWH(30.f);
        totalSample.setGridTotalFeedInKWH(40.f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(10)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        totalSample.setInputTotalKWH(20.f);
        totalSample.setOutputTotalKWH(40.f);
        totalSample.setGridTotalConsumedKWH(60.f);
        totalSample.setGridTotalFeedInKWH(80.f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(14)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(1000);

        //set afterwards calculation;
        system.setCalculateCombinedValuesAfterwards(true);
        system = solarSystemRepository.save(system);

        SampleDTO dto1 = new SampleDTO();

        dto1.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();

        deviceDTO1.setId(1L);

        dto1.setDevices(List.of(deviceDTO1));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        deviceDTO1.setGridTotalConsumptionKWH(100000f);
        deviceDTO1.setGridTotalFeedInKWH(1000000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));


        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        deviceDTO1.setGridTotalConsumptionKWH(90000f);
        deviceDTO1.setGridTotalFeedInKWH(900000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day

        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        deviceDTO1.setGridTotalConsumptionKWH(80000f);
        deviceDTO1.setGridTotalFeedInKWH(800000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        deviceDTO1.setGridTotalConsumptionKWH(60000f);
        deviceDTO1.setGridTotalFeedInKWH(600000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        influxTaskService.runUpdateTotalValues(system);

        var sys = solarSystemRepository.findAll().get(0);

        Assertions.assertThat(sys.getTotalValues().getProducedKWH()).isEqualTo(210);
        Assertions.assertThat(sys.getTotalValues().getConsumedKWH()).isEqualTo(2020);
        Assertions.assertThat(sys.getTotalValues().getGridConsumedKWH()).isEqualTo(20030);
        Assertions.assertThat(sys.getTotalValues().getGridFeedInKWH()).isEqualTo(200040);
        Assertions.assertThat(sys.getTotalValues().getProducedKWHPrice()).isEqualTo(21);
        Assertions.assertThat(sys.getTotalValues().getConsumedKWHPrice()).isEqualTo(202);
        Assertions.assertThat(sys.getTotalValues().getGridConsumedKWHPrice()).isEqualTo(2003);
        Assertions.assertThat(sys.getTotalValues().getGridFeedInKWHPrice()).isEqualTo(2000400);

        //check daily values
        var statisticDTO = doRestRequest("api/influx/latest?systemId="+sys.getId()+"&duration=3000","", HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonObject jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonObject();

        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("producedKWH").getAsFloat()).isEqualTo(210f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("consumedKWH").getAsFloat()).isEqualTo(2020f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridConsumedKWH").getAsFloat()).isEqualTo(20030f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridFeedInKWH").getAsFloat()).isEqualTo(200040f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("producedKWHPrice").getAsFloat()).isEqualTo(21f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("consumedKWHPrice").getAsFloat()).isEqualTo(202f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridConsumedKWHPrice").getAsFloat()).isEqualTo(2003f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridFeedInKWHPrice").getAsFloat()).isEqualTo(2000400);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("producedKWHDay").getAsFloat()).isEqualTo(10f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("consumedKWHDay").getAsFloat()).isEqualTo(20f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridConsumedKWHDay").getAsFloat()).isEqualTo(30f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridFeedInKWHDay").getAsFloat()).isEqualTo(40f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("producedKWHPriceDay").getAsFloat()).isEqualTo(1f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("consumedKWHPriceDay").getAsFloat()).isEqualTo(2f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridConsumedKWHPriceDay").getAsFloat()).isEqualTo(3f);
        Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get("gridFeedInKWHPriceDay").getAsFloat()).isEqualTo(400f);
    }

    @Test
    void checkTotalCalculationWithCaluclateTotal() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setElectricityPrice(0.1f);
        system.setCalculateCombinedValuesAfterwards(true);
        system = solarSystemRepository.save(system);

        influxService.updatePrice(system, ZonedDateTime.ofInstant(Instant.now().minus(Duration.ofDays(2)),ZoneId.of("UTC")));
        SampleDTO dto1 = new SampleDTO();
        dto1.setDuration(360.f);

        SampleDTO dto2 = new SampleDTO();
        dto2.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();
        deviceDTO1.setId(1L);
        dto1.setDevices(List.of(deviceDTO1));

        var deviceDTO2 = new DeviceDTO();
        deviceDTO2.setId(2L);
        dto2.setDevices(List.of(deviceDTO2));

        deviceDTO1.setInputWatt(10000000f);
        deviceDTO1.setOutputWatt(20000000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(100f);
        deviceDTO2.setOutputTotalKWH(200f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(200f);
        deviceDTO2.setOutputTotalKWH(400f);
        dto2.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(1000);

        //prev day
        deviceDTO1.setInputWatt(100000000f);
        deviceDTO1.setOutputWatt(200000000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(10f);
        deviceDTO2.setOutputTotalKWH(20f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO2.setInputTotalKWH(20f);
        deviceDTO2.setOutputTotalKWH(40f);
        dto2.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),dto2, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(5 * 1000);

        influxTaskService.runUpdateTotalValues(system);

        var sys = solarSystemRepository.findAll().get(0);

        Assertions.assertThat(Math.round(sys.getTotalValues().getProducedKWH())).isEqualTo(11110);
        Assertions.assertThat(Math.round(sys.getTotalValues().getConsumedKWH())).isEqualTo(22220);
        Assertions.assertThat(Math.round(sys.getTotalValues().getProducedKWHPrice())).isEqualTo(1111);
        Assertions.assertThat(Math.round(sys.getTotalValues().getConsumedKWHPrice())).isEqualTo(2222);

        //check daily values
        var statisticDTO = doRestRequest("api/influx/latest?systemId="+sys.getId()+"&duration=3000","", HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));

        LOG.info(statisticDTO.getBody());

        JsonObject jsonObject = JsonParser.parseString(statisticDTO.getBody()).getAsJsonObject();

        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("producedKWH").getAsInt()).isEqualTo(11110);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("consumedKWH").getAsInt()).isEqualTo(22220);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("producedKWHPrice").getAsInt()).isEqualTo(1111);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("consumedKWHPrice").getAsInt()).isEqualTo(2222);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("producedKWHDay").getAsInt()).isEqualTo(1100);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("consumedKWHDay").getAsInt()).isEqualTo(2200);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("producedKWHPriceDay").getAsInt()).isEqualTo(110);
        Assertions.assertThat(jsonObject.get("totalData").getAsJsonObject().get("consumedKWHPriceDay").getAsInt()).isEqualTo(220);
    }

    @Test
    void checkCombinedStatisticData() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user1 = addUser(true,"test1");
        var user2 = addUser(true,"test2");
        var jwt1 = signIn("test1");
        var jwt2 = signIn("test2");
        var system1 = addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        system1.setPublicMode(PublicMode.ALL);
        system1 = solarSystemRepository.save(system1);
        var system2 = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");
        system2.setCalculateCombinedValuesAfterwards(true);
        system2.setPublicMode(PublicMode.ALL);
        system2 = solarSystemRepository.save(system2);

        SampleDTO totalSample = new SampleDTO();
        totalSample.setDuration(30.f);
        totalSample.setInputTotalKWH(10.f);
        totalSample.setOutputTotalKWH(20.f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(10)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        totalSample.setInputTotalKWH(20.f);
        totalSample.setOutputTotalKWH(40.f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(14)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        totalSample.setDuration(30.f);
        totalSample.setInputTotalKWH(1.f);
        totalSample.setOutputTotalKWH(2.f);
        totalSample.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(10)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        totalSample.setInputTotalKWH(2.f);
        totalSample.setOutputTotalKWH(4.f);
        totalSample.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(14)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system1.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(1000);

        //set afterwarts calculation;

        SampleDTO dto1 = new SampleDTO();

        dto1.setDuration(300.f);

        var deviceDTO1 = new DeviceDTO();

        deviceDTO1.setId(1L);

        dto1.setDevices(List.of(deviceDTO1));

        deviceDTO1.setInputTotalKWH(1000f);
        deviceDTO1.setOutputTotalKWH(10000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));


        deviceDTO1.setInputTotalKWH(900f);
        deviceDTO1.setOutputTotalKWH(9000f);
        dto1.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        //prev day
        deviceDTO1.setInputTotalKWH(800f);
        deviceDTO1.setOutputTotalKWH(8000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        deviceDTO1.setInputTotalKWH(600f);
        deviceDTO1.setOutputTotalKWH(6000f);
        dto1.setTimestamp(startOfDay.minus(Duration.ofDays(1)).plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system2.getId(),dto1, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(10 * 1000);

        //update statistics
        doRestRequest("api/system/statistics/"+system1.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt1));
        doRestRequest("api/system/statistics/"+system2.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt2));

        Thread.sleep(5 * 1000);

        var statisticDTO = doRestRequest("api/influx/combined/statistics/all?SystemIds="+system1.getId()+","+system2.getId()+"&from="+startOfDay.minus(Duration.ofDays(6)).toEpochMilli()+"&to="+startOfDay.plus(Duration.ofDays(1)).toEpochMilli(),"", HttpMethod.GET);

        LOG.info(statisticDTO.getBody());

        JsonArray jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced_0").getAsFloat()).isEqualTo(1.f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced_1").getAsFloat()).isEqualTo(200.f);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.plus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced_0").getAsFloat()).isEqualTo(10.f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced_1").getAsFloat()).isEqualTo(100.f);

        statisticDTO = doRestRequest("api/influx/combined/statistics/latest?SystemIds="+system1.getId()+","+system2.getId(),"", HttpMethod.GET);

        LOG.info(statisticDTO.getBody());

        jsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

        Assertions.assertThat(jsonArray.size()).isEqualTo(2);

        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.toEpochMilli());
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced_0").getAsFloat()).isEqualTo(1.f);
        Assertions.assertThat(jsonArray.get(0).getAsJsonObject().get("Produced_1").getAsFloat()).isEqualTo(200.f);

        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("time").getAsLong()).isEqualTo(startOfDay.plus(Duration.ofDays(1)).toEpochMilli());
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced_0").getAsFloat()).isEqualTo(10.f);
        Assertions.assertThat(jsonArray.get(1).getAsJsonObject().get("Produced_1").getAsFloat()).isEqualTo(100.f);
    }
}