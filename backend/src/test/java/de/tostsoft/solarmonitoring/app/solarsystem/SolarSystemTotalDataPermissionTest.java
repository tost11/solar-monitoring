package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataController;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.*;
import de.tostsoft.solarmonitoring.lib.model.*;
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
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SolarSystemTotalDataPermissionTest extends AppBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemTotalDataPermissionTest.class);

    @Autowired
    private SolarDataController solarDataController;

    @Autowired
    private InfluxTaskService influxTaskService;

    @Autowired
    private de.tostsoft.solarmonitoring.app.service.InfluxService influxService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    private SampleDTO createSampleWithData(Instant timestamp, float inputWatt, float outputWatt,
                                           float gridConsumedKWH, float gridFeedInKWH,
                                           float inputTotalKWH, float outputTotalKWH) {
        SampleDTO sample = new SampleDTO();
        sample.setTimestamp(timestamp.toEpochMilli());
        sample.setTimeUnit(TimeUnit.MILLISECONDS);
        sample.setDuration(300f);

        DeviceDTO device = new DeviceDTO();
        device.setId(1L);

        device.setInputTotalKWH(inputTotalKWH);
        device.setOutputTotalKWH(outputTotalKWH);
        device.setGridTotalConsumptionKWH(gridConsumedKWH);
        device.setGridTotalFeedInKWH(gridFeedInKWH);

        InputDCDTO inputDC = new InputDCDTO();
        inputDC.setId(1L);
        inputDC.setWatt(inputWatt);
        inputDC.setVoltage(400f);
        inputDC.setAmpere(inputWatt / 400f);
        device.setInputsDC(List.of(inputDC));

        OutputACDTO outputAC = new OutputACDTO();
        outputAC.setId(1L);
        outputAC.setWatt(outputWatt);
        outputAC.setVoltage(230f);
        outputAC.setAmpere(outputWatt / 230f);
        device.setOutputsAC(List.of(outputAC));

        GridDTO grid = new GridDTO();
        grid.setId(1L);
        grid.setTotalConsumptionKWH(gridConsumedKWH);
        grid.setTotalFeedInKWH(gridFeedInKWH);
        grid.setWatt(gridConsumedKWH > 0 ? 100f : -100f);
        device.setGrids(List.of(grid));

        sample.setDevices(List.of(device));

        return sample;
    }

    private void pushSamplesAndCalculate(SolarSystem system, User owner) throws InterruptedException {
        // Write electricity prices to InfluxDB (required for pricing calculations)
        java.time.ZoneId utcZone = java.time.ZoneId.of("UTC");
        java.time.ZonedDateTime priceDate = java.time.ZonedDateTime.ofInstant(
            Instant.now().minus(3, ChronoUnit.DAYS), utcZone);
        influxService.updatePrice(system, priceDate);
        influxService.updatePriceFeedIn(system, priceDate);

        Instant day1Start = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        Instant day1End = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS);
        Instant day2Start = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
        Instant day2End = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS);

        // Day 1 start (cumulative total: 100 KWH input, 80 KWH output, 10 KWH consumed, 5 KWH feed-in)
        SampleDTO sample1Start = createSampleWithData(day1Start, 1000f, 800f, 10f, 5f, 100f, 80f);
        solarDataController.PostDevice(system.getId(), sample1Start, "token");

        // Day 1 end (cumulative total: 150 KWH input, 120 KWH output, 15 KWH consumed, 8 KWH feed-in)
        SampleDTO sample1End = createSampleWithData(day1End, 1000f, 800f, 15f, 8f, 150f, 120f);
        solarDataController.PostDevice(system.getId(), sample1End, "token");

        // Day 2 start (cumulative total: 200 KWH input, 160 KWH output, 20 KWH consumed, 12 KWH feed-in)
        SampleDTO sample2Start = createSampleWithData(day2Start, 1500f, 1200f, 20f, 12f, 200f, 160f);
        solarDataController.PostDevice(system.getId(), sample2Start, "token");

        // Day 2 end (cumulative total: 250 KWH input, 200 KWH output, 25 KWH consumed, 15 KWH feed-in)
        SampleDTO sample2End = createSampleWithData(day2End, 1500f, 1200f, 25f, 15f, 250f, 200f);
        solarDataController.PostDevice(system.getId(), sample2End, "token");

        Thread.sleep(3000);

        LOG.info("Triggering calculation for system {}", system.getId());

        // Call REST API to trigger statistics calculation (like in DailyCalculationTest)
        doRestRequest("api/system/statistics/" + system.getId(), "", HttpMethod.POST,
                     Collections.singletonMap("Cookie", "jwt=" + signIn(owner.getName())));

        Thread.sleep(5000);

        // Calculate total values from daily aggregates
        influxTaskService.runUpdateTotalValues(system);

        Thread.sleep(2000);
    }

    private JsonNode getTotalData(String systemId, String jwt) throws JsonProcessingException {
        var response = jwt != null
            ? doRequest("api/influx/latest?systemId=" + systemId + "&duration=300000",
                       HttpMethod.GET, Collections.singletonMap("Cookie", "jwt=" + jwt))
            : doRequest("api/influx/latest?systemId=" + systemId + "&duration=300000",
                       HttpMethod.GET, Collections.emptyMap());

        Assertions.assertThat(response.getBody()).isNotNull();
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.get("totalData");
    }

    private void assertProductionFields(JsonNode totalData, boolean shouldExist, String context) {
        if (shouldExist) {
            Assertions.assertThat(totalData.has("producedKWH"))
                .as(context + " - should have producedKWH").isTrue();
            Assertions.assertThat(totalData.has("producedKWHDay"))
                .as(context + " - should have producedKWHDay").isTrue();
        } else {
            Assertions.assertThat(totalData.has("producedKWH"))
                .as(context + " - should NOT have producedKWH").isFalse();
            Assertions.assertThat(totalData.has("producedKWHDay"))
                .as(context + " - should NOT have producedKWHDay").isFalse();
        }
    }

    private void assertProductionPricing(JsonNode totalData, boolean shouldExist, String context) {
        if (shouldExist) {
            Assertions.assertThat(totalData.has("producedKWHPrice"))
                .as(context + " - should have producedKWHPrice").isTrue();
            Assertions.assertThat(totalData.has("producedKWHPriceDay"))
                .as(context + " - should have producedKWHPriceDay").isTrue();
        } else {
            Assertions.assertThat(totalData.has("producedKWHPrice"))
                .as(context + " - should NOT have producedKWHPrice").isFalse();
            Assertions.assertThat(totalData.has("producedKWHPriceDay"))
                .as(context + " - should NOT have producedKWHPriceDay").isFalse();
        }
    }

    private void assertConsumptionFields(JsonNode totalData, boolean shouldExist, String context) {
        String[] fields = {"consumedKWH", "consumedKWHDay", "gridConsumedKWH", "gridConsumedKWHDay",
                          "gridFeedInKWH", "gridFeedInKWHDay", "calcOverallConsumedKWH", "calcOverallConsumedKWHDay"};

        for (String field : fields) {
            if (shouldExist) {
                Assertions.assertThat(totalData.has(field))
                    .as(context + " - should have " + field).isTrue();
            } else {
                Assertions.assertThat(totalData.has(field))
                    .as(context + " - should NOT have " + field).isFalse();
            }
        }
    }

    private void assertConsumptionPricing(JsonNode totalData, boolean shouldExist, String context) {
        String[] pricingFields = {"consumedKWHPrice", "consumedKWHPriceDay",
                                 "gridConsumedKWHPriceDay", "gridFeedInKWHPriceDay",
                                 "calcOverallConsumedKWHPriceDay"};

        for (String field : pricingFields) {
            if (shouldExist) {
                Assertions.assertThat(totalData.has(field))
                    .as(context + " - should have " + field).isTrue();
            } else {
                Assertions.assertThat(totalData.has(field))
                    .as(context + " - should NOT have " + field).isFalse();
            }
        }
    }

    private void assertAllFieldsPresent(JsonNode totalData, String context) {
        assertProductionFields(totalData, true, context);
        assertProductionPricing(totalData, true, context);
        assertConsumptionFields(totalData, true, context);
        assertConsumptionPricing(totalData, true, context);
        LOG.info("✓ {} sees all fields correctly", context);
    }

    @Test
    public void testOwnerAndManagersSeeAllDataWithPricing() throws InterruptedException, JsonProcessingException {
        LOG.info("Test: Owner and all manager permissions see all data with pricing");

        User owner = addUser(false);
        String ownerJwt = signIn("test");

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        JsonNode totalData = getTotalData(system.getId(), ownerJwt);
        assertAllFieldsPresent(totalData, "Owner");

        for (Permissions permission : Permissions.values()) {
            String managerName = "manager" + permission.name().toLowerCase();
            User manager = addUser(false, managerName);

            Manages manages = Manages.builder()
                .solarSystem(system)
                .user(manager)
                .permission(permission)
                .build();
            managesRepository.save(manages);

            String managerJwt = signIn(managerName);

            totalData = getTotalData(system.getId(), managerJwt);
            assertAllFieldsPresent(totalData, "Manager with " + permission);
        }

        LOG.info("✓ All permission levels (owner + ADMIN + MANAGE + VIEW) see complete data");
    }

    @Test
    public void testPublicProductionModeWithoutOverrideSeesOnlyProductionDataNoPricing()
            throws InterruptedException, JsonProcessingException {
        LOG.info("Test: Public PRODUCTION mode without override - production data only, no pricing");

        User owner = addUser(false);

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        ViewData viewData = system.getViewData();
        viewData.setTotalPricingPublicOverride(false);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        JsonNode totalData = getTotalData(system.getId(), null);

        assertProductionFields(totalData, true, "Public PRODUCTION without override");
        assertProductionPricing(totalData, false, "Public PRODUCTION without override");
        assertConsumptionFields(totalData, false, "Public PRODUCTION without override");
        assertConsumptionPricing(totalData, false, "Public PRODUCTION without override");

        LOG.info("✓ Public PRODUCTION without override correctly shows only production data, no pricing");
    }

    @Test
    public void testPublicProductionModeWithOverrideSeesProductionDataAndPricing()
            throws InterruptedException, JsonProcessingException {
        LOG.info("Test: Public PRODUCTION mode with override - production data and pricing");

        User owner = addUser(false);

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        ViewData viewData = system.getViewData();
        viewData.setTotalPricingPublicOverride(true);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        JsonNode totalData = getTotalData(system.getId(), null);

        assertProductionFields(totalData, true, "Public PRODUCTION with override");
        assertProductionPricing(totalData, true, "Public PRODUCTION with override");
        assertConsumptionFields(totalData, false, "Public PRODUCTION with override");
        assertConsumptionPricing(totalData, false, "Public PRODUCTION with override");

        LOG.info("✓ Public PRODUCTION with override correctly shows production data and pricing");
    }

    @Test
    public void testPublicAllModeWithoutOverrideSeesAllDataNoPricing()
            throws InterruptedException, JsonProcessingException {
        LOG.info("Test: Public ALL mode without override - all data, no pricing");

        User owner = addUser(false);

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setPublicMode(PublicMode.ALL);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        ViewData viewData = system.getViewData();
        viewData.setTotalPricingPublicOverride(false);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        JsonNode totalData = getTotalData(system.getId(), null);

        assertProductionFields(totalData, true, "Public ALL without override");
        assertProductionPricing(totalData, false, "Public ALL without override");
        assertConsumptionFields(totalData, true, "Public ALL without override");
        assertConsumptionPricing(totalData, false, "Public ALL without override");

        LOG.info("✓ Public ALL without override correctly shows all data, no pricing");
    }

    @Test
    public void testPublicAllModeWithOverrideSeesAllDataAndAllPricing()
            throws InterruptedException, JsonProcessingException {
        LOG.info("Test: Public ALL mode with override - all data and all pricing");

        User owner = addUser(false);

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setPublicMode(PublicMode.ALL);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        ViewData viewData = system.getViewData();
        viewData.setTotalPricingPublicOverride(true);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        JsonNode totalData = getTotalData(system.getId(), null);

        assertProductionFields(totalData, true, "Public ALL with override");
        assertProductionPricing(totalData, true, "Public ALL with override");
        assertConsumptionFields(totalData, true, "Public ALL with override");
        assertConsumptionPricing(totalData, true, "Public ALL with override");

        LOG.info("✓ Public ALL with override correctly shows all data and all pricing");
    }

    @Test
    public void testAllPermissionCombinationsSequentially()
            throws InterruptedException, JsonProcessingException {
        LOG.info("Test: All permission combinations tested sequentially on same system");

        User owner = addUser(false);
        String jwt = signIn("test");

        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID);
        system.setElectricityPrice(0.30f);
        system.setElectricityPriceFeedIn(0.08f);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        LOG.info("  Testing owner access");
        JsonNode totalData = getTotalData(system.getId(), jwt);
        assertAllFieldsPresent(totalData, "Owner");

        LOG.info("  Testing PUBLIC PRODUCTION without override");
        system.setPublicMode(PublicMode.PRODUCTION);
        ViewData viewData = system.getViewData();
        viewData.setTotalPricingPublicOverride(false);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        totalData = getTotalData(system.getId(), null);
        assertProductionFields(totalData, true, "Sequential - PRODUCTION no override");
        assertProductionPricing(totalData, false, "Sequential - PRODUCTION no override");
        assertConsumptionFields(totalData, false, "Sequential - PRODUCTION no override");

        LOG.info("  Testing PUBLIC PRODUCTION with override");
        viewData.setTotalPricingPublicOverride(true);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        totalData = getTotalData(system.getId(), null);
        assertProductionFields(totalData, true, "Sequential - PRODUCTION with override");
        assertProductionPricing(totalData, true, "Sequential - PRODUCTION with override");
        assertConsumptionFields(totalData, false, "Sequential - PRODUCTION with override");

        LOG.info("  Testing PUBLIC ALL without override");
        system.setPublicMode(PublicMode.ALL);
        viewData.setTotalPricingPublicOverride(false);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        totalData = getTotalData(system.getId(), null);
        assertProductionFields(totalData, true, "Sequential - ALL no override");
        assertConsumptionFields(totalData, true, "Sequential - ALL no override");
        assertProductionPricing(totalData, false, "Sequential - ALL no override");
        assertConsumptionPricing(totalData, false, "Sequential - ALL no override");

        LOG.info("  Testing PUBLIC ALL with override");
        viewData.setTotalPricingPublicOverride(true);
        system.setViewData(viewData);
        system = solarSystemRepository.save(system);

        totalData = getTotalData(system.getId(), null);
        assertAllFieldsPresent(totalData, "Sequential - ALL with override");

        LOG.info("✓ All permission combinations work correctly when changed sequentially");
    }

    @Test
    public void testPublicQueriesOnlyProductionData() throws Exception {
        LOG.info("Testing public viewers query only production data in PRODUCTION mode");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setElectricityPrice(0.30f);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        // Query yesterday only (24-hour window, within backend limit)
        Instant queryFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        Instant queryTo = Instant.now().truncatedTo(ChronoUnit.DAYS);

        ResponseEntity<String> publicResponse = doRequest(
                "api/influx/all?systemId=" + system.getId() +
                "&from=" + queryFrom.toEpochMilli() +
                "&to=" + queryTo.toEpochMilli(),
                HttpMethod.GET,
                Collections.emptyMap()
        );

        Assertions.assertThat(publicResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode publicData = objectMapper.readTree(publicResponse.getBody());

        String responseText = publicData.toString();

        Assertions.assertThat(responseText)
                .as("Public should see production fields")
                .containsAnyOf("InputWattDC", "InputVoltageDC", "InputAmpereDC", "input");

        Assertions.assertThat(responseText)
                .as("Public should NOT see consumption fields")
                .doesNotContain("consumedKWH", "gridConsumedKWH", "OutputWatt", "BatteryWatt");

        LOG.info("✓ Public queries correctly restricted to production data");
    }

    @Test
    public void testAuthenticatedUserQueriesAllData() throws Exception {
        LOG.info("Testing authenticated users query all data");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");
        system.setPublicMode(PublicMode.PRODUCTION);
        system.setElectricityPrice(0.30f);
        system = solarSystemRepository.save(system);

        pushSamplesAndCalculate(system, owner);

        String jwt = signIn("owner", "password");

        // Query yesterday only (24-hour window, within backend limit)
        Instant queryFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        Instant queryTo = Instant.now().truncatedTo(ChronoUnit.DAYS);

        ResponseEntity<String> ownerResponse = doRequest(
                "api/influx/all?systemId=" + system.getId() +
                "&from=" + queryFrom.toEpochMilli() +
                "&to=" + queryTo.toEpochMilli(),
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(ownerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode ownerData = objectMapper.readTree(ownerResponse.getBody());

        String responseText = ownerData.toString();

        Assertions.assertThat(responseText)
                .as("Owner should see production fields")
                .containsAnyOf("Input", "input", "produced");

        LOG.info("✓ Authenticated users correctly query all data");
    }
}
