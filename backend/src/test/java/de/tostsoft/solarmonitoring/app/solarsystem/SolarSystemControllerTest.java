package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.EditSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NamingsDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SystemInformationsDTO;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class SolarSystemControllerTest extends AppBaseTest {

    @MockBean
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Value("${system.defaultMaxSamplesDay}")
    private long defaultMaxSamplesDaySysgtem;

    public static EditSolarSystemDTO crateDefaultRegisterDTO(){
        SystemInformationsDTO systemInfo = SystemInformationsDTO.builder()
            .name("test")
            .publicName(null)
            .description(null)
            .electricityPrice(0.3f)
            .electricityPriceFeedIn(0.1f)
            .maxInstalledSolarPower(10f)
            .maxInverterOutputPower(8f)
            .batteryCapacity(null)
            .buildingDate(null)
            .build();

        return EditSolarSystemDTO.builder()
            .id(null)
            .token(null)
            .shortener(null)
            .type(SolarSystemType.GRID)
            .systemInformations(systemInfo)
            .viewData(ViewDataDTO.builder()
                .hasTemperature(false)
                .defaultDelay(0)
                .showGridInfo(false)
                .build())
            .timezone("UTC")
            .publicMode(PublicMode.NONE)
            .namings(NamingsDTO.builder()
                .devices(new HashMap<>())
                .batteries(new HashMap<>())
                .inputsAC(new HashMap<>())
                .inputsDC(new HashMap<>())
                .outputsAC(new HashMap<>())
                .outputsDC(new HashMap<>())
                .grids(new HashMap<>())
                .build())
            .deyeSunSerialNumbers(null)
            .calculateCombinedValuesAfterwards(false)
            .build();
    }

    @Test
    public void checkAdminSystemUnlimitedSamples(){
        addUser(true);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);
        Assertions.assertThat(system.getMaxSamplesOnDay()).isEqualTo(-1);
    }

    @Test
    public void checkNotAdminSystemLimitedSamples(){
        addUser(false);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);
        Assertions.assertThat(system.getMaxSamplesOnDay()).isEqualTo(defaultMaxSamplesDaySysgtem);
    }

    @Test
    public void checkCreationCorrect(){
        addUser(true);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();
        systemDTO.getSystemInformations().setName("Test");
        systemDTO.setType(SolarSystemType.GRID);
        systemDTO.setPublicMode(PublicMode.NONE);
        systemDTO.getSystemInformations().setBuildingDate(ZonedDateTime.now());
        systemDTO.setTimezone("UTC");
        systemDTO.setCalculateCombinedValuesAfterwards(false);
        systemDTO.setDeyeSunSerialNumbers("123456789");
        systemDTO.getSystemInformations().setElectricityPrice(0.33f);
        systemDTO.getSystemInformations().setElectricityPriceFeedIn(0.66f);
        systemDTO.getSystemInformations().setMaxInstalledSolarPower(5000f);
        systemDTO.getSystemInformations().setMaxInverterOutputPower(3000f);
        systemDTO.getSystemInformations().setPublicName("Test Public Name");
        systemDTO.getSystemInformations().setDescription("<p>Test <b>HTML</b> description</p>");
        systemDTO.getSystemInformations().setBatteryCapacity(15.5f);
        systemDTO.setShortener("tes");
        systemDTO.getViewData().setBatteryVoltage(12);
        systemDTO.getViewData().setMaxSolarVoltage(60);
        systemDTO.getViewData().setVoltageAC(230);
        systemDTO.getViewData().setDefaultDelay(300);
        systemDTO.getViewData().setHasTemperature(true);
        systemDTO.getViewData().setHideTotalConsumption(true);
        systemDTO.getViewData().setShowGridInfo(true);
        systemDTO.getViewData().setTotalPricingPublicOverride(true);
        systemDTO.getViewData().setProductionForTotalPricing(true);
        systemDTO.getViewData().setTotalFilter(Set.of("CalcProducedKWH", "CalcByDevicesConsumedKWH"));
        systemDTO.getViewData().setGraphFilter(Set.of(
            GraphFilter.INPUT_FREQUENCY,
            GraphFilter.OUTPUT_AMPERE_AC,
            GraphFilter.BATTERY_SOC
        ));

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);

        Assertions.assertThat(system.getName()).isEqualTo("test");
        Assertions.assertThat(system.getViewName()).isEqualTo("Test");
        Assertions.assertThat(system.getType()).isEqualTo(SolarSystemType.GRID);
        Assertions.assertThat(system.getPublicMode()).isEqualTo(PublicMode.NONE);
        Assertions.assertThat(system.getTimezone()).isEqualTo("UTC");
        Assertions.assertThat(system.getCalculateCombinedValuesAfterwards()).isFalse();
        Assertions.assertThat(system.getDeyeSunSerials()).contains(123456789L);
        Assertions.assertThat(system.getElectricityPrice()).isEqualTo(0.33f);
        Assertions.assertThat(system.getElectricityPriceFeedIn()).isEqualTo(0.66f);
        Assertions.assertThat(system.getMaxInstalledSolarPower()).isEqualTo(5000f);
        Assertions.assertThat(system.getMaxInverterOutputPower()).isEqualTo(3000f);
        Assertions.assertThat(system.getShortener()).isEqualTo("tes");
        Assertions.assertThat(system.getViewData().getBatteryVoltage()).isEqualTo(12);
        Assertions.assertThat(system.getViewData().getMaxSolarVoltage()).isEqualTo(60);
        Assertions.assertThat(system.getViewData().getVoltageAC()).isEqualTo(230);
        Assertions.assertThat(system.getViewData().getDefaultDelay()).isEqualTo(300);
        Assertions.assertThat(system.getViewData().getHasTemperature()).isTrue();
        Assertions.assertThat(system.getViewData().getHideTotalConsumption()).isTrue();
        Assertions.assertThat(system.getViewData().getShowGridInfo()).isTrue();
        Assertions.assertThat(system.getViewData().getTotalPricingPublicOverride()).isTrue();
        Assertions.assertThat(system.getViewData().getProductionForTotalPricing()).isTrue();
        Assertions.assertThat(system.getViewData().getTotalFilter())
            .containsExactlyInAnyOrder("CalcProducedKWH", "CalcByDevicesConsumedKWH");
        Assertions.assertThat(system.getViewData().getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.OUTPUT_AMPERE_AC,
                GraphFilter.BATTERY_SOC
            );
        Assertions.assertThat(system.getSystemInformations().getName()).isEqualTo("test");
        Assertions.assertThat(system.getSystemInformations().getViewName()).isEqualTo("Test");
        Assertions.assertThat(system.getSystemInformations().getPublicName()).isEqualTo("Test Public Name");
        Assertions.assertThat(system.getSystemInformations().getDescription()).isEqualTo("<p>Test <b>HTML</b> description</p>");
        Assertions.assertThat(system.getSystemInformations().getBatteryCapacity()).isEqualTo(15.5f);
        Assertions.assertThat(system.getSystemInformations().getBuildingDate()).isNotNull();
    }

    @Test
    public void checkCreateGetPatchGetRoundTrip() throws JsonProcessingException, InterruptedException {
        when(influxTaskService.runInitial(any(),(ThreadPoolExecutor)any())).thenReturn(false);

        addUser(true);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();
        systemDTO.getSystemInformations().setName("RoundTripTest");
        systemDTO.setType(SolarSystemType.GRID_BATTERY);
        systemDTO.setPublicMode(PublicMode.PRODUCTION);
        systemDTO.getSystemInformations().setBuildingDate(ZonedDateTime.now());
        systemDTO.getSystemInformations().setPublicName("Round Trip Public");
        systemDTO.getSystemInformations().setDescription("<p>Initial <i>description</i> with <a href=\"http://example.com\">link</a></p>");
        systemDTO.getSystemInformations().setBatteryCapacity(20.0f);
        systemDTO.setTimezone("Europe/Berlin");
        systemDTO.setCalculateCombinedValuesAfterwards(true);
        systemDTO.setDeyeSunSerialNumbers("111,222,333");
        systemDTO.getSystemInformations().setElectricityPrice(0.30f);
        systemDTO.getSystemInformations().setElectricityPriceFeedIn(0.08f);
        systemDTO.getSystemInformations().setMaxInstalledSolarPower(8000f);
        systemDTO.getSystemInformations().setMaxInverterOutputPower(6000f);
        systemDTO.setShortener("rtt");
        systemDTO.getViewData().setBatteryVoltage(48);
        systemDTO.getViewData().setMaxSolarVoltage(120);
        systemDTO.getViewData().setVoltageAC(230);
        systemDTO.getViewData().setDefaultDelay(60);
        systemDTO.getViewData().setHasTemperature(false);
        systemDTO.getViewData().setHideTotalConsumption(false);
        systemDTO.getViewData().setShowGridInfo(true);
        systemDTO.getViewData().setTotalPricingPublicOverride(false);
        systemDTO.getViewData().setProductionForTotalPricing(true);
        systemDTO.getViewData().setTotalFilter(Set.of("CalcConsumedKWH"));
        systemDTO.getViewData().setGraphFilter(Set.of(GraphFilter.values()));
        systemDTO.getNamings().getDevices().put("1", "Inverter 1");
        systemDTO.getNamings().getBatteries().put("0-1", "Battery Bank");
        systemDTO.getNamings().getGrids().put("0-1", "Main Grid");

        var createResponseStr = doRestRequest("api/system", systemDTO, HttpMethod.POST,
            Collections.singletonMap("Cookie","jwt="+jwt));

        Assertions.assertThat(createResponseStr.getBody()).isNotNull();
        var createResponse = objectMapper.readValue(createResponseStr.getBody(), EditSolarSystemDTO.class);
        Assertions.assertThat(createResponse).isNotNull();
        Assertions.assertThat(createResponse.getId()).isNotNull();
        Assertions.assertThat(createResponse.getToken()).isNotNull();
        Assertions.assertThat(createResponse.getSystemInformations().getName()).isEqualTo("RoundTripTest");
        Assertions.assertThat(createResponse.getType()).isEqualTo(SolarSystemType.GRID_BATTERY);
        Assertions.assertThat(createResponse.getPublicMode()).isEqualTo(PublicMode.PRODUCTION);
        Assertions.assertThat(createResponse.getSystemInformations().getElectricityPrice()).isEqualTo(0.30f);
        Assertions.assertThat(createResponse.getSystemInformations().getElectricityPriceFeedIn()).isEqualTo(0.08f);
        Assertions.assertThat(createResponse.getSystemInformations().getPublicName()).isEqualTo("Round Trip Public");
        Assertions.assertThat(createResponse.getSystemInformations().getDescription())
            .isEqualTo("<p>Initial <i>description</i> with <a href=\"http://example.com\" rel=\"nofollow\">link</a></p>");
        Assertions.assertThat(createResponse.getSystemInformations().getBatteryCapacity()).isEqualTo(20.0f);
        Assertions.assertThat(createResponse.getSystemInformations().getBuildingDate()).isNotNull();
        Assertions.assertThat(createResponse.getViewData()).isNotNull();
        Assertions.assertThat(createResponse.getViewData().getShowGridInfo()).isTrue();
        Assertions.assertThat(createResponse.getViewData().getBatteryVoltage()).isEqualTo(48);
        Assertions.assertThat(createResponse.getNamings()).isNotNull();
        Assertions.assertThat(createResponse.getNamings().getDevices()).containsEntry("1", "Inverter 1");

        var systemId = createResponse.getId();

        var getResponseStr = doRequest("api/system/" + systemId, HttpMethod.GET,
            Collections.singletonMap("Cookie","jwt="+jwt));
        Assertions.assertThat(getResponseStr.getBody()).isNotNull();
        var getResponse = objectMapper.readValue(getResponseStr.getBody(), SolarSystemDTO.class);

        Assertions.assertThat(getResponse).isNotNull();
        Assertions.assertThat(getResponse.getName()).isEqualTo("RoundTripTest");
        Assertions.assertThat(getResponse.getType()).isEqualTo(SolarSystemType.GRID_BATTERY);
        Assertions.assertThat(getResponse.getPublicMode()).isEqualTo(PublicMode.PRODUCTION);
        Assertions.assertThat(getResponse.getTimezone()).isEqualTo("Europe/Berlin");
        Assertions.assertThat(getResponse.getCalculateCombinedValuesAfterwards()).isTrue();
        Assertions.assertThat(getResponse.getDeyeSunSerialNumbers()).contains("111", "222", "333");
        Assertions.assertThat(getResponse.getElectricityPrice()).isEqualTo(0.30f);
        Assertions.assertThat(getResponse.getElectricityPriceFeedIn()).isEqualTo(0.08f);
        Assertions.assertThat(getResponse.getMaxInstalledSolarPower()).isEqualTo(8000f);
        Assertions.assertThat(getResponse.getMaxInverterOutputPower()).isEqualTo(6000f);
        Assertions.assertThat(getResponse.getShortener()).isEqualTo("rtt");
        Assertions.assertThat(getResponse.getViewData().getBatteryVoltage()).isEqualTo(48);
        Assertions.assertThat(getResponse.getViewData().getMaxSolarVoltage()).isEqualTo(120);
        Assertions.assertThat(getResponse.getViewData().getVoltageAC()).isEqualTo(230);
        Assertions.assertThat(getResponse.getViewData().getDefaultDelay()).isEqualTo(60);
        Assertions.assertThat(getResponse.getViewData().getHasTemperature()).isFalse();
        Assertions.assertThat(getResponse.getViewData().getHideTotalConsumption()).isFalse();
        Assertions.assertThat(getResponse.getViewData().getShowGridInfo()).isTrue();
        Assertions.assertThat(getResponse.getViewData().getTotalPricingPublicOverride()).isFalse();
        Assertions.assertThat(getResponse.getViewData().getProductionForTotalPricing()).isTrue();
        Assertions.assertThat(getResponse.getViewData().getTotalFilter()).containsExactly("CalcConsumedKWH");
        Assertions.assertThat(getResponse.getViewData().getGraphFilter())
            .isNotNull()
            .hasSize(GraphFilter.values().length)
            .containsExactlyInAnyOrder(GraphFilter.values());
        Assertions.assertThat(getResponse.getNamings().getDevices()).containsEntry("1", "Inverter 1");
        Assertions.assertThat(getResponse.getNamings().getBatteries()).containsEntry("0-1", "Battery Bank");
        Assertions.assertThat(getResponse.getNamings().getGrids()).containsEntry("0-1", "Main Grid");
        Assertions.assertThat(getResponse.getDescription())
            .contains("Initial")
            .contains("description")
            .contains("example.com");
        Assertions.assertThat(getResponse.getBatteryCapacity()).isEqualTo(20.0f);
        Assertions.assertThat(getResponse.getBuildingDate()).isNotNull();

        SystemInformationsDTO patchInfo = SystemInformationsDTO.builder()
            .name("RoundTripTest Updated")
            .buildingDate(getResponse.getBuildingDate())
            .electricityPrice(0.35f)
            .electricityPriceFeedIn(0.10f)
            .maxInstalledSolarPower(10000f)
            .maxInverterOutputPower(8000f)
            .publicName("Updated Public Name")
            .description("<ul><li>Updated</li><li>List</li></ul>")
            .batteryCapacity(25.0f)
            .build();

        var patchDTO = EditSolarSystemDTO.builder()
            .id(systemId)
            .type(SolarSystemType.SELFMADE)
            .publicMode(PublicMode.ALL)
            .systemInformations(patchInfo)
            .timezone("UTC")
            .calculateCombinedValuesAfterwards(false)
            .deyeSunSerialNumbers("444,555")
            .shortener("rtt2")
            .viewData(ViewDataDTO.builder()
                .batteryVoltage(24)
                .maxSolarVoltage(100)
                .voltageAC(240)
                .defaultDelay(120)
                .hasTemperature(true)
                .hideTotalConsumption(true)
                .showGridInfo(false)
                .totalPricingPublicOverride(true)
                .productionForTotalPricing(false)
                .totalFilter(Set.of("CalcProducedKWH", "CalcGridFeedInKWH"))
                .graphFilter(Set.of(
                    GraphFilter.OUTPUT_WATT_DC,
                    GraphFilter.BATTERY_AMPERE,
                    GraphFilter.MORE_TEMPERATURE
                ))
                .build())
            .namings(NamingsDTO.builder()
                .devices(new HashMap<>())
                .batteries(new HashMap<>())
                .grids(new HashMap<>())
                .inputsAC(new HashMap<>())
                .inputsDC(new HashMap<>())
                .outputsAC(new HashMap<>())
                .outputsDC(new HashMap<>())
                .build())
            .build();

        patchDTO.getNamings().getDevices().put("1", "Inverter 1 Updated");
        patchDTO.getNamings().getDevices().put("2", "Inverter 2");
        patchDTO.getNamings().getGrids().put("0-1", "Main Grid Updated");

        doRestRequest("api/system/edit", patchDTO, HttpMethod.POST,
            Collections.singletonMap("Cookie","jwt="+jwt));

        Thread.sleep(1000);

        var getResponse2Str = doRequest("api/system/" + systemId, HttpMethod.GET,
            Collections.singletonMap("Cookie","jwt="+jwt));

        Assertions.assertThat(getResponse2Str.getBody()).isNotNull();
        var getResponse2 = objectMapper.readValue(getResponse2Str.getBody(), SolarSystemDTO.class);

        Assertions.assertThat(getResponse2).isNotNull();
        Assertions.assertThat(getResponse2.getName()).isEqualTo("RoundTripTest Updated");
        Assertions.assertThat(getResponse2.getType()).isEqualTo(SolarSystemType.SELFMADE);
        Assertions.assertThat(getResponse2.getPublicMode()).isEqualTo(PublicMode.ALL);
        Assertions.assertThat(getResponse2.getTimezone()).isEqualTo("UTC");
        Assertions.assertThat(getResponse2.getCalculateCombinedValuesAfterwards()).isFalse();
        Assertions.assertThat(getResponse2.getDeyeSunSerialNumbers()).contains("444", "555");
        Assertions.assertThat(getResponse2.getElectricityPrice()).isEqualTo(0.35f);
        Assertions.assertThat(getResponse2.getElectricityPriceFeedIn()).isEqualTo(0.10f);
        Assertions.assertThat(getResponse2.getMaxInstalledSolarPower()).isEqualTo(10000f);
        Assertions.assertThat(getResponse2.getMaxInverterOutputPower()).isEqualTo(8000f);
        Assertions.assertThat(getResponse2.getShortener()).isEqualTo("rtt2");
        Assertions.assertThat(getResponse2.getViewData().getBatteryVoltage()).isEqualTo(24);
        Assertions.assertThat(getResponse2.getViewData().getMaxSolarVoltage()).isEqualTo(100);
        Assertions.assertThat(getResponse2.getViewData().getVoltageAC()).isEqualTo(240);
        Assertions.assertThat(getResponse2.getViewData().getDefaultDelay()).isEqualTo(120);
        Assertions.assertThat(getResponse2.getViewData().getHasTemperature()).isTrue();
        Assertions.assertThat(getResponse2.getViewData().getHideTotalConsumption()).isTrue();
        Assertions.assertThat(getResponse2.getViewData().getShowGridInfo()).isFalse();
        Assertions.assertThat(getResponse2.getViewData().getTotalPricingPublicOverride()).isTrue();
        Assertions.assertThat(getResponse2.getViewData().getProductionForTotalPricing()).isFalse();
        Assertions.assertThat(getResponse2.getViewData().getTotalFilter())
            .containsExactlyInAnyOrder("CalcProducedKWH", "CalcGridFeedInKWH");
        Assertions.assertThat(getResponse2.getViewData().getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.OUTPUT_WATT_DC,
                GraphFilter.BATTERY_AMPERE,
                GraphFilter.MORE_TEMPERATURE
            );
        Assertions.assertThat(getResponse2.getNamings().getDevices()).containsEntry("1", "Inverter 1 Updated");
        Assertions.assertThat(getResponse2.getNamings().getDevices()).containsEntry("2", "Inverter 2");
        Assertions.assertThat(getResponse2.getNamings().getGrids()).containsEntry("0-1", "Main Grid Updated");
        Assertions.assertThat(getResponse2.getNamings().getBatteries()).isEmpty();
        Assertions.assertThat(getResponse2.getDescription())
            .contains("<ul>")
            .contains("<li>Updated</li>")
            .contains("<li>List</li>");
        Assertions.assertThat(getResponse2.getBatteryCapacity()).isEqualTo(25.0f);
        Assertions.assertThat(getResponse2.getBuildingDate()).isNotNull();
        Assertions.assertThat(getResponse2.getBuildingDate()).isEqualTo(getResponse.getBuildingDate());
    }

}
