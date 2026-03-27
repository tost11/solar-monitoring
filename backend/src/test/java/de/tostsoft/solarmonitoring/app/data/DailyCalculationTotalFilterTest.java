package de.tostsoft.solarmonitoring.app.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.TotalValues;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.offset;

public class DailyCalculationTotalFilterTest  extends AppBaseTest {

    @Autowired
    private InfluxService influxService;

    @Autowired
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    private interface Extractor{
        float extract(TotalValues totalValues);
    }

    void checkValuesWithFilter(SolarSystem system,String jwt,Instant startOfDay, String param,String statString, List<String> filters,Extractor extractor,float expectedValue) throws InterruptedException {

        //system.getViewData().setTotalFilter(new HashSet<>(Arrays.asList("ProducedKWH","CalcByDevicesProducedKWH","CalcProducedKWH")));
        system.getViewData().setTotalFilter(new HashSet<>(filters));
        solarSystemRepository.save(system);

        //his is important and i don't know why...
        doRestRequest("api/system/statistics/"+system.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        influxTaskService.runUpdateTotalValues(system);

        Thread.sleep(1000);

        var sys = solarSystemRepository.findAll().get(0);

        Assertions.assertThat(extractor.extract(sys.getTotalValues())).isCloseTo(expectedValue,offset(0.001f));

        //check daily values
        var normalDTO = doRestRequest("api/influx/latest?systemId="+sys.getId()+"&duration=3000","", HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));

        JsonObject jsonArray = JsonParser.parseString(normalDTO.getBody()).getAsJsonObject();

        if(expectedValue <= 0.f) {
            Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get(param).isJsonNull()).isTrue();
        }else {
            Assertions.assertThat(jsonArray.get("totalData").getAsJsonObject().get(param).getAsFloat()).isCloseTo(expectedValue, offset(0.001f));
        }

        if(statString != null) {
            var statisticDTO = doRestRequest("api/influx/statistics/all?systemId=" + system.getId() + "&from=" + startOfDay.minus(Duration.ofDays(6)).toEpochMilli() + "&to=" + startOfDay.plus(Duration.ofDays(1)).toEpochMilli(), "", HttpMethod.GET, Collections.singletonMap("Cookie", "jwt=" + jwt));

            JsonArray statJsonArray = JsonParser.parseString(statisticDTO.getBody()).getAsJsonArray();

            Assertions.assertThat(statJsonArray.size()).isEqualTo(1);

            Assertions.assertThat(statJsonArray.get(0).getAsJsonObject().get(statString).getAsFloat()).isCloseTo(expectedValue, offset(0.001f));
            //TODO statis for deivce (if fixed)
        }
    }

    @Test
    void checkDailyCalculationCorrect() throws InterruptedException {

        ZoneId z = ZoneId.of( "UTC" ) ;
        LocalDate today = LocalDate.now(z) ;
        Instant startOfDay = today.atStartOfDay(ZoneId.of( "UTC" )).toInstant();

        var user = addUser(false);
        var jwt = signIn();
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setElectricityPrice(1000f);
        system.setElectricityPriceFeedIn(10000f);
        system = solarSystemRepository.save(system);

        influxService.updatePrice(system, ZonedDateTime.ofInstant(Instant.now().minus(Duration.ofDays(2)),ZoneId.of("UTC")));
        influxService.updatePriceFeedIn(system, ZonedDateTime.ofInstant(Instant.now().minus(Duration.ofDays(2)),ZoneId.of("UTC")));

        SampleDTO totalSample = new SampleDTO();
        var device = new DeviceDTO();
        device.setId(1L);
        totalSample.setDuration(36f);
        totalSample.setDevices(Collections.singletonList(device));

        totalSample.setInputTotalKWH(10.f);
        totalSample.setInputWatt(50000f);//evaluate to 1KWH
        device.setInputTotalKWH(100f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(10)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        totalSample.setInputTotalKWH(20.f);
        totalSample.setInputWatt(50000f);
        device.setInputTotalKWH(200f);
        totalSample.setTimestamp(startOfDay.plus(Duration.ofHours(14)).toEpochMilli());
        doRestRequest("api/solar/data?systemId="+system.getId(),totalSample, HttpMethod.POST, Map.of("clientToken","token"));

        Thread.sleep(1000);

        checkValuesWithFilter(system,jwt,startOfDay,"producedKWH","Produced",new ArrayList<>(),TotalValues::getProducedKWH,10f);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWHPrice",null,new ArrayList<>(),TotalValues::getProducedKWHPrice,10 * 1000f);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWH","Produced",List.of("ProducedKWH"),TotalValues::getProducedKWH,100);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWHPrice",null,List.of("ProducedKWHPrice"),TotalValues::getProducedKWHPrice,100 * 1000);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWH","Produced",Arrays.asList("ProducedKWH","CalcByDevicesProducedKWH"),TotalValues::getProducedKWH,1.f);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWHPrice",null, Arrays.asList("ProducedKWHPrice","CalcByDevicesProducedKWHPrice"),TotalValues::getProducedKWHPrice,1.f * 1000);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWH","Produced",Arrays.asList("ProducedKWH","CalcByDevicesProducedKWH","CalcProducedKWH"),TotalValues::getProducedKWH,0.f);
        checkValuesWithFilter(system,jwt,startOfDay,"producedKWHPrice",null,Arrays.asList("ProducedKWHPrice","CalcByDevicesProducedKWHPrice","CalcProducedKWHPrice"),TotalValues::getProducedKWHPrice,0.f);
    }
}
