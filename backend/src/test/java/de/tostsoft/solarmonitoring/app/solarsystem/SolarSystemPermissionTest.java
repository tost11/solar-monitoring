package de.tostsoft.solarmonitoring.app.solarsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PublicSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.Set;
import org.springframework.http.ResponseEntity;

public class SolarSystemPermissionTest extends AppBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemPermissionTest.class);

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testIntegrationProductionModePermissions() throws JsonProcessingException {
        User owner = addUser(false, "owner");
        User admin = addUser(false, "admin");
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

        Manages adminManages = Manages.builder()
            .solarSystem(system)
            .user(admin)
            .permission(Permissions.ADMIN)
            .build();
        managesRepository.save(adminManages);

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
        Assertions.assertThat(adminJson.get("maxInverterOutputPower").asDouble()).isEqualTo(5000.0);

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

    @Test
    public void testShowGridInfoFalseHidesGridGraphs() throws Exception {
        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        ViewData viewData = system.getViewData();
        viewData.setShowGridInfo(false);
        viewData.setGraphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.GRID_WATT,
                GraphFilter.GRID_VOLTAGE,
                GraphFilter.GRID_AMPERE,
                GraphFilter.GRID_FREQUENCY
        ));
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        var response = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt));
        var dto = objectMapper.readTree(response.getBody());
        var resultViewData = dto.get("viewData");
        var graphFilter = objectMapper.convertValue(resultViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(graphFilter)
                .as("Grid filters should be removed when showGridInfo=false")
                .doesNotContain("GRID_WATT", "GRID_VOLTAGE", "GRID_AMPERE", "GRID_FREQUENCY")
                .contains("INPUT_FREQUENCY");
    }

    @Test
    public void testShowGridInfoTrueShowsGridGraphs() throws Exception {
        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        ViewData viewData = system.getViewData();
        viewData.setShowGridInfo(true);
        viewData.setGraphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.GRID_WATT,
                GraphFilter.GRID_VOLTAGE
        ));
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        var response = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt));
        var dto = objectMapper.readTree(response.getBody());
        var resultViewData = dto.get("viewData");
        var graphFilter = objectMapper.convertValue(resultViewData.get("graphFilter"), Set.class);

        Assertions.assertThat(graphFilter)
                .as("All grid filters should be present when showGridInfo=true")
                .containsExactlyInAnyOrder("INPUT_FREQUENCY", "GRID_WATT", "GRID_VOLTAGE");
    }

    @Test
    public void testHasTemperatureFalseHidesTemperatureSection() throws Exception {
        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        ViewData viewData = system.getViewData();
        viewData.setHasTemperature(false);
        viewData.setGraphFilter(Set.of(
                GraphFilter.MORE_TEMPERATURE,
                GraphFilter.INPUT_FREQUENCY
        ));
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        var response = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt));
        var dto = objectMapper.readTree(response.getBody());
        var resultViewData = dto.get("viewData");

        Assertions.assertThat(resultViewData.get("hasTemperature").asBoolean())
                .as("hasTemperature should be false")
                .isFalse();

        var graphFilter = objectMapper.convertValue(resultViewData.get("graphFilter"), Set.class);
        Assertions.assertThat(graphFilter)
                .as("MORE_TEMPERATURE should be removed when hasTemperature=false")
                .doesNotContain("MORE_TEMPERATURE")
                .contains("INPUT_FREQUENCY");
    }

    @Test
    public void testHasTemperatureTrueShowsTemperatureSection() throws Exception {
        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        ViewData viewData = system.getViewData();
        viewData.setHasTemperature(true);
        viewData.setGraphFilter(Set.of(
                GraphFilter.MORE_TEMPERATURE,
                GraphFilter.INPUT_FREQUENCY
        ));
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        var response = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt));
        var dto = objectMapper.readTree(response.getBody());
        var resultViewData = dto.get("viewData");

        Assertions.assertThat(resultViewData.get("hasTemperature").asBoolean())
                .as("hasTemperature should be true")
                .isTrue();

        var graphFilter = objectMapper.convertValue(resultViewData.get("graphFilter"), Set.class);
        Assertions.assertThat(graphFilter)
                .as("MORE_TEMPERATURE should be present when hasTemperature=true")
                .containsExactlyInAnyOrder("MORE_TEMPERATURE", "INPUT_FREQUENCY");
    }

    @Test
    public void testHasTemperatureFalseForPublicInProductionMode() throws Exception {
        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");
        system.setPublicMode(PublicMode.PRODUCTION);

        ViewData viewData = system.getViewData();
        viewData.setHasTemperature(true);
        viewData.setGraphFilter(Set.of(
                GraphFilter.MORE_TEMPERATURE,
                GraphFilter.INPUT_FREQUENCY
        ));
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        var publicResponse = doRequest("api/system/public/" + system.getId(), HttpMethod.GET,
                Collections.emptyMap());
        var publicDto = objectMapper.readTree(publicResponse.getBody());
        var publicViewData = publicDto.get("viewData");

        Assertions.assertThat(publicViewData.get("hasTemperature").asBoolean())
                .as("hasTemperature should be false for public viewers in PRODUCTION mode")
                .isFalse();

        var publicGraphFilter = objectMapper.convertValue(publicViewData.get("graphFilter"), Set.class);
        Assertions.assertThat(publicGraphFilter)
                .as("MORE_TEMPERATURE should be removed for public viewers")
                .doesNotContain("MORE_TEMPERATURE");
    }

    @Test
    public void testPublicModeNoneDeniesPublicAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE denies public access");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        final SolarSystem savedSystem = solarSystemRepository.save(system);

        assertThatThrownBy(() -> doRequest(
            "api/system/public/" + savedSystem.getId(),
            HttpMethod.GET,
            Collections.emptyMap()
        ))
            .as("Public access to NONE mode system should return 403")
            .hasMessageContaining("403");

        LOG.info("✓ PublicMode.NONE correctly denies public access (HTTP 403)");
    }

    @Test
    public void testPublicModeNoneAllowsOwnerAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE allows owner access");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        ResponseEntity<String> response = doRequest(
            "api/system/" + system.getId(),
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        assertThat(response.getStatusCode().is2xxSuccessful())
            .as("Owner should be able to access NONE mode system")
            .isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.has("id")).as("Response should contain system data").isTrue();
        assertThat(root.get("id").asText()).isEqualTo(system.getId());

        LOG.info("✓ PublicMode.NONE correctly allows owner access");
    }

    @Test
    public void testPublicModeNoneAllowsExplicitPermissionAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE allows access for users with explicit permissions");

        User owner = addUser(false, "owner");
        User viewer = addUser(false, "viewer");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        system = solarSystemRepository.save(system);

        Manages manages = Manages.builder()
            .solarSystem(system)
            .user(viewer)
            .permission(Permissions.VIEW)
            .build();
        managesRepository.save(manages);

        String viewerJwt = signIn("viewer", "password");
        ResponseEntity<String> response = doRequest(
            "api/system/" + system.getId(),
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + viewerJwt)
        );

        assertThat(response.getStatusCode().is2xxSuccessful())
            .as("User with VIEW permission should access NONE mode system")
            .isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.get("id").asText()).isEqualTo(system.getId());

        LOG.info("✓ PublicMode.NONE correctly allows access for users with explicit permissions");
    }

    @Test
    public void testPublicModeNoneHidesSystemFromPublicListing() throws Exception {
        LOG.info("Testing PublicMode.NONE hides system from public listings");

        User owner = addUser(false, "owner");

        SolarSystem publicSystem = addSolarSystemForUser(owner, SolarSystemType.GRID, "public-system");
        publicSystem.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(publicSystem);

        SolarSystem privateSystem = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        privateSystem.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(privateSystem);

        assertThatThrownBy(() -> doRequest(
            "api/system/public",
            HttpMethod.GET,
            Collections.emptyMap()
        ))
            .as("Public listing with only NONE mode systems should return 403")
            .hasMessageContaining("403");

        LOG.info("✓ PublicMode.NONE correctly hides system from public listings");
    }
}
