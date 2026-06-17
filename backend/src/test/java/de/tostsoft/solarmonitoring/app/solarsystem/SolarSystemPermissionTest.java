package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PublicSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SolarSystemPermissionTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testOwnerSeesAllFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, true);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            );
    }

    @Test
    public void testPublicViewerProductionModeRemovesConsumptionFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(1)
            .containsExactly(GraphFilter.INPUT_FREQUENCY);
    }

    @Test
    public void testPublicViewerAllModeKeepsAllFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.ALL, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            );
    }

    @Test
    public void testProductionModeWithNoConsumptionFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC
            );
    }

    @Test
    public void testProductionModeWithAllConsumptionFilters() {
        Set<GraphFilter> allConsumptionFilters = Set.of(
            GraphFilter.OUTPUT_WATT_DC,
            GraphFilter.OUTPUT_WATT_AC,
            GraphFilter.OUTPUT_WATT_COMBINED,
            GraphFilter.OUTPUT_VOLTAGE_DC,
            GraphFilter.OUTPUT_VOLTAGE_AC,
            GraphFilter.OUTPUT_AMPERE_DC,
            GraphFilter.OUTPUT_AMPERE_AC,
            GraphFilter.OUTPUT_FREQUENCY,
            GraphFilter.OUTPUT_TOTAL_CONSUMPTION,
            GraphFilter.BATTERY_WATT,
            GraphFilter.BATTERY_VOLTAGE,
            GraphFilter.BATTERY_AMPERE,
            GraphFilter.BATTERY_SOC,
            GraphFilter.GRID_WATT,
            GraphFilter.GRID_VOLTAGE,
            GraphFilter.GRID_AMPERE,
            GraphFilter.GRID_FREQUENCY,
            GraphFilter.MORE_TEMPERATURE
        );

        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(new HashSet<>(allConsumptionFilters))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void testProductionModeMixedFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC,
                GraphFilter.INPUT_WATT_DC,
                GraphFilter.BATTERY_SOC,
                GraphFilter.BATTERY_VOLTAGE,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(4)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC,
                GraphFilter.INPUT_WATT_DC
            );
    }

    @Test
    public void testNullGraphFilter() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(null)
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter()).isNull();
    }

    @Test
    public void testEmptyGraphFilter() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of())
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void testIntegrationProductionModePermissions() throws JsonProcessingException {
        User owner = addUser(false, "owner");
        User admin = addUser(true, "admin");
        User viewer = addUser(false, "viewer");
        User manager = addUser(false, "manager");

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID_BATTERY, "testsystem");
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setViewData(ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            ))
            .build());
        system.getSystemInformations().setMaxInstalledSolarPower(8000f);
        system.getSystemInformations().setMaxInverterOutputPower(5000f);
        system = solarSystemRepository.save(system);

        Manages viewManages = Manages.builder()
            .solarSystem(system)
            .user(viewer)
            .permission(Permissions.VIEW)
            .build();
        managesRepository.save(viewManages);

        Manages manageManages = Manages.builder()
            .solarSystem(system)
            .user(manager)
            .permission(Permissions.MANAGE)
            .build();
        managesRepository.save(manageManages);

        String ownerJwt = signIn("owner", "password");
        var ownerResponse = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + ownerJwt));
        var ownerViewData = objectMapper.readTree(ownerResponse.getBody()).get("viewData");
        var ownerGraphFilter = objectMapper.convertValue(ownerViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(ownerGraphFilter)
            .hasSize(5)
            .containsExactlyInAnyOrder("INPUT_FREQUENCY", "INPUT_VOLTAGE_DC", "BATTERY_SOC", "GRID_WATT", "OUTPUT_WATT_AC");

        var ownerJson = objectMapper.readTree(ownerResponse.getBody());
        Assertions.assertThat(ownerJson.has("maxInstalledSolarPower")).isTrue();
        Assertions.assertThat(ownerJson.get("maxInstalledSolarPower").asDouble()).isEqualTo(8000.0);
        Assertions.assertThat(ownerJson.has("maxInverterOutputPower")).isTrue();
        Assertions.assertThat(ownerJson.get("maxInverterOutputPower").asDouble()).isEqualTo(5000.0);

        //TODO enable if admin access feature is implemented
        /*
        String adminJwt = signIn("admin", "password");
        var adminResponse = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + adminJwt));
        var adminViewData = objectMapper.readTree(adminResponse.getBody()).get("viewData");
        var adminGraphFilter = objectMapper.convertValue(adminViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(adminGraphFilter)
            .hasSize(5)
            .containsExactlyInAnyOrder("INPUT_FREQUENCY", "INPUT_VOLTAGE_DC", "BATTERY_SOC", "GRID_WATT", "OUTPUT_WATT_AC");

        var adminJson = objectMapper.readTree(adminResponse.getBody());
        Assertions.assertThat(adminJson.has("maxInstalledSolarPower")).isTrue();
        Assertions.assertThat(adminJson.get("maxInstalledSolarPower").asDouble()).isEqualTo(8000.0);
        Assertions.assertThat(adminJson.has("maxInverterOutputPower")).isTrue();
        Assertions.assertThat(adminJson.get("maxInverterOutputPower").asDouble()).isEqualTo(5000.0);*/

        String viewerJwt = signIn("viewer", "password");
        var viewerResponse = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + viewerJwt));
        var viewerViewData = objectMapper.readTree(viewerResponse.getBody()).get("viewData");
        var viewerGraphFilter = objectMapper.convertValue(viewerViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(viewerGraphFilter)
            .hasSize(5)
            .containsExactlyInAnyOrder("INPUT_FREQUENCY", "INPUT_VOLTAGE_DC", "BATTERY_SOC", "GRID_WATT", "OUTPUT_WATT_AC");

        var viewerJson = objectMapper.readTree(viewerResponse.getBody());
        Assertions.assertThat(viewerJson.has("maxInstalledSolarPower")).isTrue();
        Assertions.assertThat(viewerJson.get("maxInstalledSolarPower").asDouble()).isEqualTo(8000.0);
        Assertions.assertThat(viewerJson.has("maxInverterOutputPower")).isTrue();
        Assertions.assertThat(viewerJson.get("maxInverterOutputPower").asDouble()).isEqualTo(5000.0);

        String managerJwt = signIn("manager", "password");
        var managerResponse = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + managerJwt));
        var managerViewData = objectMapper.readTree(managerResponse.getBody()).get("viewData");
        var managerGraphFilter = objectMapper.convertValue(managerViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(managerGraphFilter)
            .hasSize(5)
            .containsExactlyInAnyOrder("INPUT_FREQUENCY", "INPUT_VOLTAGE_DC", "BATTERY_SOC", "GRID_WATT", "OUTPUT_WATT_AC");

        var managerJson = objectMapper.readTree(managerResponse.getBody());
        Assertions.assertThat(managerJson.has("maxInstalledSolarPower")).isTrue();
        Assertions.assertThat(managerJson.get("maxInstalledSolarPower").asDouble()).isEqualTo(8000.0);
        Assertions.assertThat(managerJson.has("maxInverterOutputPower")).isTrue();
        Assertions.assertThat(managerJson.get("maxInverterOutputPower").asDouble()).isEqualTo(5000.0);

        var publicResponse = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Collections.emptyMap());
        var publicDTO = objectMapper.readValue(publicResponse.getBody(), PublicSolarSystemDTO.class);

        Assertions.assertThat(publicDTO.getViewData().getGraphFilter())
            .hasSize(2)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC
            );

        Assertions.assertThat(publicDTO.getMaxInstalledSolarPower()).isEqualTo(8000f);
        var publicJson = objectMapper.readTree(publicResponse.getBody());
        Assertions.assertThat(publicJson.has("maxInverterOutputPower")).isFalse();

        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        var publicResponseAll = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Collections.emptyMap());
        var publicDTOAll = objectMapper.readValue(publicResponseAll.getBody(), PublicSolarSystemDTO.class);

        Assertions.assertThat(publicDTOAll.getViewData().getGraphFilter())
            .hasSize(5)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            );

        Assertions.assertThat(publicDTOAll.getMaxInstalledSolarPower()).isEqualTo(8000f);
        var publicJsonAll = objectMapper.readTree(publicResponseAll.getBody());
        Assertions.assertThat(publicJsonAll.has("maxInverterOutputPower")).isFalse();
    }

    @Test
    public void testIntegrationPermissionChanges() throws JsonProcessingException {
        User owner = addUser(false, "owner");
        User user1 = addUser(false, "user1");

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID_BATTERY, "testsystem");
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setViewData(ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_WATT_DC,
                GraphFilter.BATTERY_VOLTAGE,
                GraphFilter.GRID_FREQUENCY,
                GraphFilter.OUTPUT_VOLTAGE_AC
            ))
            .build());
        system = solarSystemRepository.save(system);

        Manages manages = Manages.builder()
            .solarSystem(system)
            .user(user1)
            .permission(Permissions.VIEW)
            .build();
        manages = managesRepository.save(manages);

        String user1Jwt = signIn("user1", "password");
        var response1 = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + user1Jwt));
        var viewData1 = objectMapper.readTree(response1.getBody()).get("viewData");
        var graphFilter1 = objectMapper.convertValue(viewData1.get("graphFilter"), Set.class);

        Assertions.assertThat(graphFilter1)
            .hasSize(4)
            .containsExactlyInAnyOrder("INPUT_WATT_DC", "BATTERY_VOLTAGE", "GRID_FREQUENCY", "OUTPUT_VOLTAGE_AC");

        manages.setPermission(Permissions.MANAGE);
        managesRepository.save(manages);

        var response2 = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + user1Jwt));
        var viewData2 = objectMapper.readTree(response2.getBody()).get("viewData");
        var graphFilter2 = objectMapper.convertValue(viewData2.get("graphFilter"), Set.class);

        Assertions.assertThat(graphFilter2)
            .hasSize(4)
            .containsExactlyInAnyOrder("INPUT_WATT_DC", "BATTERY_VOLTAGE", "GRID_FREQUENCY", "OUTPUT_VOLTAGE_AC");

        manages.setPermission(Permissions.ADMIN);
        managesRepository.save(manages);

        var response3 = doRequest("api/system/" + system.getId(), HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + user1Jwt));
        var viewData3 = objectMapper.readTree(response3.getBody()).get("viewData");
        var graphFilter3 = objectMapper.convertValue(viewData3.get("graphFilter"), Set.class);

        Assertions.assertThat(graphFilter3)
            .hasSize(4)
            .containsExactlyInAnyOrder("INPUT_WATT_DC", "BATTERY_VOLTAGE", "GRID_FREQUENCY", "OUTPUT_VOLTAGE_AC");
    }
}
