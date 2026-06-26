package de.tostsoft.solarmonitoring.app.tag;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.dto.SystemContributionDTO;
import de.tostsoft.solarmonitoring.lib.dto.TagAggregationDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TagAggregationTest extends AppBaseTest {

    @Autowired
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    /**
     * Creates a solar system with a specific tag and public mode
     */
    private SolarSystem createSystemWithTag(User owner, Tag tag, String systemName,
                                           PublicMode publicMode, SolarSystemType type) {
        SolarSystem system = addSolarSystemForUser(owner, type, systemName);
        system.setPublicMode(publicMode);
        // Initialize tags list if null
        if (system.getTags() == null) {
            system.setTags(new java.util.ArrayList<>());
        }
        system.getTags().add(tag);  // Add Tag object, not just ID
        system.setCurrentValues(CurrentValues.builder().build());
        return solarSystemRepository.save(system);
    }

    /**
     * Pushes two samples (start and end of day) with cumulative total values.
     * The daily total is calculated as: end - start (using spread function).
     * Also updates current values for real-time data.
     */
    private void pushDailySamplesWithDevice(SolarSystem system, Instant startOfDay,
                                           float startInputTotal, float endInputTotal,
                                           float startOutputTotal, float endOutputTotal,
                                           Float startGridFeedInTotal, Float endGridFeedInTotal,
                                           Float startGridConsumptionTotal, Float endGridConsumptionTotal,
                                           float currentInputWatt, float currentOutputWatt,
                                           Float currentGridWatt, boolean isOnline) throws Exception {

        // Create device DTOs (using device ID 1)
        DeviceDTO deviceStart = new DeviceDTO();
        deviceStart.setId(1L);
        deviceStart.setInputTotalKWH(startInputTotal);
        deviceStart.setOutputTotalKWH(startOutputTotal);
        if (startGridFeedInTotal != null) {
            deviceStart.setGridTotalFeedInKWH(startGridFeedInTotal);
        }
        if (startGridConsumptionTotal != null) {
            deviceStart.setGridTotalConsumptionKWH(startGridConsumptionTotal);
        }

        DeviceDTO deviceEnd = new DeviceDTO();
        deviceEnd.setId(1L);
        deviceEnd.setInputTotalKWH(endInputTotal);
        deviceEnd.setOutputTotalKWH(endOutputTotal);
        if (endGridFeedInTotal != null) {
            deviceEnd.setGridTotalFeedInKWH(endGridFeedInTotal);
        }
        if (endGridConsumptionTotal != null) {
            deviceEnd.setGridTotalConsumptionKWH(endGridConsumptionTotal);
        }

        // Sample 1: Start of day (01:00)
        SampleDTO sampleStart = new SampleDTO();
        sampleStart.setDuration(300.f);
        sampleStart.setDevices(List.of(deviceStart));
        sampleStart.setTimestamp(startOfDay.plus(Duration.ofHours(1)).toEpochMilli());
        doRestRequest("api/solar/data?systemId=" + system.getId(), sampleStart,
            HttpMethod.POST, Map.of("clientToken", "token"));

        // Sample 2: End of day (23:00)
        SampleDTO sampleEnd = new SampleDTO();
        sampleEnd.setDuration(300.f);
        sampleEnd.setDevices(List.of(deviceEnd));
        sampleEnd.setTimestamp(startOfDay.plus(Duration.ofHours(23)).toEpochMilli());
        doRestRequest("api/solar/data?systemId=" + system.getId(), sampleEnd,
            HttpMethod.POST, Map.of("clientToken", "token"));

        // Update current values for real-time display
        CurrentValues currentValues = system.getCurrentValues();
        if (currentValues == null) {
            currentValues = CurrentValues.builder().build();
        }
        currentValues.setInputWatt(currentInputWatt);
        currentValues.setOutputWatt(currentOutputWatt);
        if (currentGridWatt != null) {
            currentValues.setGridWatt(currentGridWatt);
        }

        // Set lastSet timestamp to control online status
        // If online, set current timestamp; if offline, set old timestamp (1 hour ago)
        if (isOnline) {
            currentValues.setLastSet(System.currentTimeMillis());
        } else {
            // Set timestamp to 1 hour ago to simulate offline
            currentValues.setLastSet(System.currentTimeMillis() - 3600 * 1000);
        }

        system.setCurrentValues(currentValues);
        solarSystemRepository.save(system);
    }

    /**
     * Triggers daily calculation and waits for it to complete.
     * Calls the statistics endpoint which runs InfluxDB Flux queries to calculate
     * daily totals using spread() function and stores results in SOLAR_DAY_DATA measurement.
     */
    private void triggerDailyCalculation(SolarSystem system, String jwt) throws Exception {
        // Wait briefly for data to be written to InfluxDB
        Thread.sleep(2 * 1000);

        // Trigger calculation via REST endpoint (needs authentication)
        doRestRequest("api/system/statistics/" + system.getId(), "", HttpMethod.POST,
            Collections.singletonMap("Cookie", "jwt=" + jwt));
    }

    @Test
    public void testBasicAggregationMultipleSystemsWithDailyTotals() throws Exception {
        // Setup
        User owner = addUser(true, "owner");
        Tag solarTag = addTag("Solar Systems", "#FFA500");

        SolarSystem system1 = createSystemWithTag(owner, solarTag, "System1",
                                                 PublicMode.ALL, SolarSystemType.GRID);
        SolarSystem system2 = createSystemWithTag(owner, solarTag, "System2",
                                                 PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        SolarSystem system3 = createSystemWithTag(owner, solarTag, "System3",
                                                 PublicMode.ALL, SolarSystemType.SELFMADE);

        // Get start of today (UTC)
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        // Push daily samples with cumulative totals (start and end of day)
        // System 1: Day production=15kWh (25-10), consumption=10kWh (15-5), grid feed-in=10kWh (13-3)
        pushDailySamplesWithDevice(system1, startOfToday,
            10.f, 25.f,     // InputTotalKWH: start=10, end=25 -> 15 kWh produced
            5.f, 15.f,      // OutputTotalKWH: start=5, end=15 -> 10 kWh consumed
            3.f, 13.f,      // GridTotalFeedInKWH: start=3, end=13 -> 10 kWh fed in
            null, null,     // No grid consumption
            5000.f, 2000.f, -3000.f, true);  // Current values and online status

        // System 2: Day production=12kWh (32-20), consumption=12kWh (22-10), grid consumption=2kWh (4-2)
        pushDailySamplesWithDevice(system2, startOfToday,
            20.f, 32.f,     // InputTotalKWH: 12 kWh produced
            10.f, 22.f,     // OutputTotalKWH: 12 kWh consumed
            null, null,     // No grid feed-in
            2.f, 4.f,       // GridTotalConsumptionKWH: 2 kWh consumed from grid
            3000.f, 4000.f, 1000.f, true);

        // System 3: Day production=3kWh (8-5), consumption=2kWh (4-2), offline
        pushDailySamplesWithDevice(system3, startOfToday,
            5.f, 8.f,       // InputTotalKWH: 3 kWh produced
            2.f, 4.f,       // OutputTotalKWH: 2 kWh consumed
            null, null,     // No grid feed-in
            null, null,     // No grid consumption
            1000.f, 500.f, null, false);  // Offline

        // Sign in for authenticated endpoint access
        String jwt = signIn("owner");

        Thread.sleep(5 * 1000);

        // Trigger daily calculation for all systems
        triggerDailyCalculation(system1, jwt);
        triggerDailyCalculation(system2, jwt);
        triggerDailyCalculation(system3, jwt);

        // Brief wait for async calculation to complete
        Thread.sleep(5 * 1000);

        // Calculate total values from daily aggregates
        influxTaskService.runUpdateTotalValues(system1);
        influxTaskService.runUpdateTotalValues(system2);
        influxTaskService.runUpdateTotalValues(system3);

        Thread.sleep(2000);

        // Call tag aggregation endpoint
        var response = doRestRequest(
            "/api/tags/aggregation/" + solarTag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // Assertions
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Parse response body
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getTag().getName()).isEqualTo("Solar Systems");
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(3);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(2);

        // Check aggregated daily totals
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(30.f);  // 15 + 12 + 3
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(24.f);  // 10 + 12 + 2

        // Check aggregated current totals (only online systems)
        Assertions.assertThat(dto.getTotalCurrentProduction()).isEqualTo(8000.f);  // 5000 + 3000 (offline excluded)
        Assertions.assertThat(dto.getTotalCurrentConsumption()).isEqualTo(6000.f); // 2000 + 4000 (offline excluded)
        Assertions.assertThat(dto.getTotalCurrentGrid()).isCloseTo(-2000.f, within(1.f)); // -3000 + 1000

        // Check per-system contributions
        Assertions.assertThat(dto.getSystems().getContent()).hasSize(3);
        Assertions.assertThat(dto.getSystems().getTotalElements()).isEqualTo(3);
        Assertions.assertThat(dto.getSystems().getPage()).isEqualTo(0);
        Assertions.assertThat(dto.getSystems().getSize()).isEqualTo(15);

        SystemContributionDTO sys1 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("System1"))
            .findFirst()
            .orElseThrow();

        // System 1 assertions
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(15.f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(10.f);
        Assertions.assertThat(sys1.getCurrentProduction()).isEqualTo(5000.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(2000.f);
        Assertions.assertThat(sys1.isOnline()).isTrue();

        // System 3 (offline) assertions
        SystemContributionDTO sys3 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("System3"))
            .findFirst()
            .orElseThrow();

        Assertions.assertThat(sys3.getDayProducedKWH()).isEqualTo(3.f);
        Assertions.assertThat(sys3.getDayConsumedKWH()).isEqualTo(2.f);
        Assertions.assertThat(sys3.isOnline()).isFalse();
    }

    @Test
    public void testEmptyTagReturnsEmptyAggregation() throws Exception {
        // Setup
        User owner = addUser(false, "owner");
        Tag emptyTag = addTag("Empty Tag", "#CCCCCC");

        String jwt = signIn("owner");

        // Call endpoint
        var response = doRestRequest(
            "/api/tags/aggregation/" + emptyTag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // Assertions
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(0);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(0);
        Assertions.assertThat(dto.getTotalCurrentProduction()).isEqualTo(0.f);
        Assertions.assertThat(dto.getSystems().getContent()).isEmpty();
        Assertions.assertThat(dto.getSystems().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void testTagNotFoundReturnsNotFound() {
        // Setup
        User owner = addUser(false, "owner");
        String jwt = signIn("owner");

        // Call endpoint with invalid tag ID
        var exception = assertThrows(HttpClientErrorException.class, () ->
            doRestRequest(
                "/api/tags/aggregation/INVALID_TAG_ID",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
            )
        );

        // Should return 404 or 400
        Assertions.assertThat(exception.getStatusCode())
            .isIn(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testPublicSystemsAccessibleByOtherUser() throws Exception {
        // Setup owner with public systems
        User owner = addUser(false, "owner");
        Tag publicTag = addTag("Public Systems", "#00FF00");

        SolarSystem system1 = createSystemWithTag(owner, publicTag, "PublicSystem1",
                                                 PublicMode.ALL, SolarSystemType.GRID);
        SolarSystem system2 = createSystemWithTag(owner, publicTag, "PublicSystem2",
                                                 PublicMode.ALL, SolarSystemType.GRID_BATTERY);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        // System 1: 10 kWh produced, 8 kWh consumed
        pushDailySamplesWithDevice(system1, startOfToday,
            5.f, 15.f,      // Production: 10 kWh
            3.f, 11.f,      // Consumption: 8 kWh
            null, null, null, null,
            2000.f, 1500.f, null, true);

        // System 2: 20 kWh produced, 15 kWh consumed
        pushDailySamplesWithDevice(system2, startOfToday,
            10.f, 30.f,     // Production: 20 kWh
            5.f, 20.f,      // Consumption: 15 kWh
            null, null, null, null,
            3000.f, 2500.f, null, true);

        // Sign in as owner to trigger calculations
        String ownerJwt = signIn("owner");
        triggerDailyCalculation(system1, ownerJwt);
        triggerDailyCalculation(system2, ownerJwt);
        Thread.sleep(5 * 1000);

        influxTaskService.runUpdateTotalValues(system1);
        influxTaskService.runUpdateTotalValues(system2);
        Thread.sleep(2000);

        // Create different user and sign in as them
        addUser(false, "otherUser");
        String otherUserJwt = signIn("otherUser");

        // Call as other user - should see public systems
        var response = doRestRequest(
            "/api/tags/aggregation/" + publicTag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + otherUserJwt)
        );

        // Assertions
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(2);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(2);
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(30.f);  // 10 + 20
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(23.f);  // 8 + 15

        // Verify both systems have PUBLIC role and consumption visible
        for (SystemContributionDTO sys : dto.getSystems().getContent()) {
            Assertions.assertThat(sys.getRole()).isEqualTo("PUBLIC");
            Assertions.assertThat(sys.getDayConsumedKWH()).isNotNull();
        }
    }

    @Test
    public void testProductionOnlyModeFiltersConsumption() throws Exception {
        // Setup owner with mixed public mode systems
        User owner = addUser(false, "owner");
        Tag mixedTag = addTag("Mixed Access", "#0000FF");

        // Create 3 systems with different public modes
        SolarSystem system1 = createSystemWithTag(owner, mixedTag, "AllDataSystem",
                                                 PublicMode.ALL, SolarSystemType.GRID);
        SolarSystem system2 = createSystemWithTag(owner, mixedTag, "ProductionOnlySystem",
                                                 PublicMode.PRODUCTION, SolarSystemType.GRID);
        SolarSystem system3 = createSystemWithTag(owner, mixedTag, "AllDataSystem2",
                                                 PublicMode.ALL, SolarSystemType.GRID_BATTERY);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        // System 1: 10 kWh produced, 8 kWh consumed (ALL - consumption visible)
        pushDailySamplesWithDevice(system1, startOfToday,
            5.f, 15.f,      // Production: 10 kWh
            3.f, 11.f,      // Consumption: 8 kWh
            null, null, null, null,
            2000.f, 1500.f, -500.f, true);

        // System 2: 15 kWh produced, 12 kWh consumed (PRODUCTION - consumption hidden)
        pushDailySamplesWithDevice(system2, startOfToday,
            10.f, 25.f,     // Production: 15 kWh
            5.f, 17.f,      // Consumption: 12 kWh (should be hidden)
            null, null, null, null,
            3000.f, 2000.f, -1000.f, true);

        // System 3: 20 kWh produced, 15 kWh consumed (ALL - consumption visible)
        pushDailySamplesWithDevice(system3, startOfToday,
            8.f, 28.f,      // Production: 20 kWh
            4.f, 19.f,      // Consumption: 15 kWh
            null, null, null, null,
            4000.f, 3000.f, 1000.f, true);

        String ownerJwt = signIn("owner");
        triggerDailyCalculation(system1, ownerJwt);
        triggerDailyCalculation(system2, ownerJwt);
        triggerDailyCalculation(system3, ownerJwt);
        Thread.sleep(5 * 1000);

        influxTaskService.runUpdateTotalValues(system1);
        influxTaskService.runUpdateTotalValues(system2);
        influxTaskService.runUpdateTotalValues(system3);
        Thread.sleep(2000);

        // Create different user and sign in as them to test public access
        addUser(false, "otherUser");
        String otherUserJwt = signIn("otherUser");

        // Call as other user - should see all 3 public systems but with filtered data
        var response = doRestRequest(
            "/api/tags/aggregation/" + mixedTag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + otherUserJwt)
        );

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(3);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(3);

        // Production visible for all systems
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(45.f);  // 10 + 15 + 20

        // Consumption only from system 1 and 3 (system 2 is PRODUCTION mode)
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(23.f);  // 8 + 15 (not 12)

        // Current consumption only from system 1 and 3
        Assertions.assertThat(dto.getTotalCurrentConsumption()).isEqualTo(4500.f);  // 1500 + 3000 (not 2000)

        // Verify system 1 (ALL) has consumption
        SystemContributionDTO sys1 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("AllDataSystem"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(8.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(1500.f);
        Assertions.assertThat(sys1.getCurrentGrid()).isEqualTo(-500.f);

        // Verify system 2 (PRODUCTION) has NO consumption
        SystemContributionDTO sys2 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("ProductionOnlySystem"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys2.getDayProducedKWH()).isEqualTo(15.f);
        Assertions.assertThat(sys2.getDayConsumedKWH()).isNull();
        Assertions.assertThat(sys2.getCurrentConsumption()).isNull();
        Assertions.assertThat(sys2.getCurrentGrid()).isNull();

        // Verify system 3 (ALL) has consumption
        SystemContributionDTO sys3 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("AllDataSystem2"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys3.getDayConsumedKWH()).isEqualTo(15.f);
        Assertions.assertThat(sys3.getCurrentConsumption()).isEqualTo(3000.f);
    }

    @Test
    public void testAnonymousAccessToPublicSystems() throws Exception {
        // Setup owner with public systems
        User owner = addUser(false, "owner");
        Tag publicTag = addTag("Anonymous Access Tag", "#FF00FF");

        SolarSystem system1 = createSystemWithTag(owner, publicTag, "PublicSystem1",
                                                 PublicMode.ALL, SolarSystemType.GRID);
        SolarSystem system2 = createSystemWithTag(owner, publicTag, "PublicSystem2",
                                                 PublicMode.PRODUCTION, SolarSystemType.GRID);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        // System 1: 12 kWh produced, 9 kWh consumed
        pushDailySamplesWithDevice(system1, startOfToday,
            5.f, 17.f,      // Production: 12 kWh
            4.f, 13.f,      // Consumption: 9 kWh
            null, null, null, null,
            2500.f, 1800.f, -700.f, true);

        // System 2: 18 kWh produced, 14 kWh consumed (consumption hidden)
        pushDailySamplesWithDevice(system2, startOfToday,
            8.f, 26.f,      // Production: 18 kWh
            6.f, 20.f,      // Consumption: 14 kWh (hidden in PRODUCTION mode)
            null, null, null, null,
            3500.f, 2200.f, -1300.f, true);

        // Sign in as owner to trigger calculations
        String ownerJwt = signIn("owner");
        triggerDailyCalculation(system1, ownerJwt);
        triggerDailyCalculation(system2, ownerJwt);
        Thread.sleep(5 * 1000);

        influxTaskService.runUpdateTotalValues(system1);
        influxTaskService.runUpdateTotalValues(system2);
        Thread.sleep(3000);

        // Call WITHOUT JWT - anonymous access
        var response = doRestRequest(
            "/api/tags/aggregation/" + publicTag.getId(),
            null,
            HttpMethod.GET
        );

        // Assertions
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(2);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(2);

        // Production visible for all systems
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(30.f);  // 12 + 18

        // Consumption only from system1 (ALL mode), not from system2 (PRODUCTION mode)
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(9.f);  // only system1

        // Current consumption only from system1
        Assertions.assertThat(dto.getTotalCurrentConsumption()).isEqualTo(1800.f);  // only system1

        // Verify system 1 (ALL) has consumption and PUBLIC role
        SystemContributionDTO sys1 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("PublicSystem1"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys1.getRole()).isEqualTo("PUBLIC");
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(12.f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(9.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(1800.f);

        // Verify system 2 (PRODUCTION) has NO consumption and PUBLIC role
        SystemContributionDTO sys2 = dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals("PublicSystem2"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys2.getRole()).isEqualTo("PUBLIC");
        Assertions.assertThat(sys2.getDayProducedKWH()).isEqualTo(18.f);
        Assertions.assertThat(sys2.getDayConsumedKWH()).isNull();
        Assertions.assertThat(sys2.getCurrentConsumption()).isNull();
        Assertions.assertThat(sys2.getCurrentGrid()).isNull();
    }

    @Test
    public void testAnonymousAccessToTagWithoutPublicSystems() {
        // Setup owner with private systems (no public mode)
        User owner = addUser(false, "owner");
        Tag privateTag = addTag("Private Tag", "#000000");

        // Create system with no public mode (NONE)
        SolarSystem system = createSystemWithTag(owner, privateTag, "PrivateSystem",
                                                PublicMode.NONE, SolarSystemType.GRID);

        // Call WITHOUT JWT - should return empty result
        var response = doRestRequest(
            "/api/tags/aggregation/" + privateTag.getId(),
            null,
            HttpMethod.GET
        );

        // Should return HTTP 200 with empty aggregation
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TagAggregationDTO dto = null;
        try {
            dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);
        } catch (Exception e) {
            Assertions.fail("Failed to parse response: " + e.getMessage());
        }

        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(0);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(0);
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(0.f);
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(0.f);
        Assertions.assertThat(dto.getSystems().getContent()).isEmpty();
        Assertions.assertThat(dto.getSystems().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void testConsumptionCalculationWithGridValuesBasedOnShowGridInfo() throws Exception {
        // Test the new consumption calculation logic that conditionally includes
        // grid values based on ViewData.showGridInfo setting

        // ARRANGE: Create test data
        User owner = addUser(true, "gridTestOwner");
        Tag solarTag = addTag("Grid Test Systems", "#00FF00");
        String jwt = signIn("gridTestOwner");

        // System 1: showGridInfo=true WITH grid values
        SolarSystem system1 = createSystemWithTag(owner, solarTag, "System1-GridEnabled",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        ViewData viewData1 = ViewData.builder()
                .showGridInfo(true)
                .build();
        system1.setViewData(viewData1);
        CurrentValues cv1 = CurrentValues.builder()
                .inputWatt(5000.f)       // Solar production
                .outputWatt(3000.f)      // Device output to house
                .gridWatt(1000.f)        // Consuming from grid (positive)
                .lastSet(System.currentTimeMillis())  // Online
                .build();
        system1.setCurrentValues(cv1);
        system1 = solarSystemRepository.save(system1);
        // Expected: currentConsumption = 3000 + 1000 = 4000W

        // System 2: showGridInfo=false WITH grid values
        SolarSystem system2 = createSystemWithTag(owner, solarTag, "System2-GridDisabled",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        ViewData viewData2 = ViewData.builder()
                .showGridInfo(false)  // Grid info disabled
                .build();
        system2.setViewData(viewData2);
        CurrentValues cv2 = CurrentValues.builder()
                .inputWatt(4000.f)
                .outputWatt(2500.f)
                .gridWatt(800.f)        // Grid value present but should be ignored
                .lastSet(System.currentTimeMillis())
                .build();
        system2.setCurrentValues(cv2);
        system2 = solarSystemRepository.save(system2);
        // Expected: currentConsumption = 2500W (gridWatt ignored)

        // System 3: showGridInfo=true WITHOUT grid values
        SolarSystem system3 = createSystemWithTag(owner, solarTag, "System3-NoGrid",
                PublicMode.ALL, SolarSystemType.SIMPLE);
        ViewData viewData3 = ViewData.builder()
                .showGridInfo(true)  // Enabled but no grid data
                .build();
        system3.setViewData(viewData3);
        CurrentValues cv3 = CurrentValues.builder()
                .inputWatt(3000.f)
                .outputWatt(2000.f)
                .gridWatt(null)         // No grid value
                .lastSet(System.currentTimeMillis())
                .build();
        system3.setCurrentValues(cv3);
        system3 = solarSystemRepository.save(system3);
        // Expected: currentConsumption = 2000W (fallback to outputWatt)

        // System 4: showGridInfo=true WITH negative grid (feeding in)
        SolarSystem system4 = createSystemWithTag(owner, solarTag, "System4-FeedIn",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        ViewData viewData4 = ViewData.builder()
                .showGridInfo(true)
                .build();
        system4.setViewData(viewData4);
        CurrentValues cv4 = CurrentValues.builder()
                .inputWatt(6000.f)
                .outputWatt(2000.f)
                .gridWatt(-500.f)       // Feeding to grid (negative)
                .lastSet(System.currentTimeMillis())
                .build();
        system4.setCurrentValues(cv4);
        system4 = solarSystemRepository.save(system4);
        // Expected: currentConsumption = max(0, 2000 + (-500)) = 1500W

        // ACT: Call aggregation endpoint
        var response = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId(),
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // ASSERT: Verify results
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        // Verify aggregated totals
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(4);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(4);

        // Total current consumption = 4000 + 2500 + 2000 + 1500 = 10000W
        Assertions.assertThat(dto.getTotalCurrentConsumption())
                .isCloseTo(10000.f, within(1.f));

        // Verify individual systems
        // System 1: Grid enabled, positive grid value
        SystemContributionDTO sys1 = dto.getSystems().getContent().stream()
                .filter(s -> s.getName().equals("System1-GridEnabled"))
                .findFirst()
                .orElseThrow();
        Assertions.assertThat(sys1.getCurrentConsumption())
                .isCloseTo(4000.f, within(0.1f));  // 3000 + 1000
        Assertions.assertThat(sys1.getCurrentGrid())
                .isCloseTo(1000.f, within(0.1f));

        // System 2: Grid disabled, grid value ignored
        SystemContributionDTO sys2 = dto.getSystems().getContent().stream()
                .filter(s -> s.getName().equals("System2-GridDisabled"))
                .findFirst()
                .orElseThrow();
        Assertions.assertThat(sys2.getCurrentConsumption())
                .isCloseTo(2500.f, within(0.1f));  // outputWatt only
        Assertions.assertThat(sys2.getCurrentGrid())
                .isCloseTo(800.f, within(0.1f));   // Grid value still returned

        // System 3: No grid value available
        SystemContributionDTO sys3 = dto.getSystems().getContent().stream()
                .filter(s -> s.getName().equals("System3-NoGrid"))
                .findFirst()
                .orElseThrow();
        Assertions.assertThat(sys3.getCurrentConsumption())
                .isCloseTo(2000.f, within(0.1f));  // Fallback to outputWatt
        Assertions.assertThat(sys3.getCurrentGrid()).isNull();

        // System 4: Negative grid (feeding in)
        SystemContributionDTO sys4 = dto.getSystems().getContent().stream()
                .filter(s -> s.getName().equals("System4-FeedIn"))
                .findFirst()
                .orElseThrow();
        Assertions.assertThat(sys4.getCurrentConsumption())
                .isCloseTo(1500.f, within(0.1f));  // max(0, 2000 + (-500))
        Assertions.assertThat(sys4.getCurrentGrid())
                .isCloseTo(-500.f, within(0.1f));  // Negative value preserved
    }

    @Test
    public void testSortingByCurrentProductionAscending() throws Exception {
        // Test that sorting by current production works correctly in ascending order

        // ARRANGE: Create test data
        User owner = addUser(true, "sortTestOwner");
        Tag solarTag = addTag("Sort Test Systems", "#FF00FF");
        String jwt = signIn("sortTestOwner");

        // Create 5 systems with different current production values
        SolarSystem sys1 = createSystemWithTag(owner, solarTag, "System-100W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys1.setCurrentValues(CurrentValues.builder()
                .inputWatt(100.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys1);

        SolarSystem sys2 = createSystemWithTag(owner, solarTag, "System-500W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys2.setCurrentValues(CurrentValues.builder()
                .inputWatt(500.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys2);

        SolarSystem sys3 = createSystemWithTag(owner, solarTag, "System-300W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys3.setCurrentValues(CurrentValues.builder()
                .inputWatt(300.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys3);

        SolarSystem sys4 = createSystemWithTag(owner, solarTag, "System-800W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys4.setCurrentValues(CurrentValues.builder()
                .inputWatt(800.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys4);

        SolarSystem sys5 = createSystemWithTag(owner, solarTag, "System-200W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys5.setCurrentValues(CurrentValues.builder()
                .inputWatt(200.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys5);

        // ACT: Call aggregation endpoint with sortBy=currentproduction, sortOrder=asc
        var response = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // ASSERT: Verify results
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        List<SystemContributionDTO> systems = dto.getSystems().getContent();
        Assertions.assertThat(systems).hasSize(5);

        // Verify systems are sorted in ascending order by current production
        Assertions.assertThat(systems.get(0).getName()).isEqualTo("System-100W");
        Assertions.assertThat(systems.get(0).getCurrentProduction()).isCloseTo(100.f, within(0.1f));

        Assertions.assertThat(systems.get(1).getName()).isEqualTo("System-200W");
        Assertions.assertThat(systems.get(1).getCurrentProduction()).isCloseTo(200.f, within(0.1f));

        Assertions.assertThat(systems.get(2).getName()).isEqualTo("System-300W");
        Assertions.assertThat(systems.get(2).getCurrentProduction()).isCloseTo(300.f, within(0.1f));

        Assertions.assertThat(systems.get(3).getName()).isEqualTo("System-500W");
        Assertions.assertThat(systems.get(3).getCurrentProduction()).isCloseTo(500.f, within(0.1f));

        Assertions.assertThat(systems.get(4).getName()).isEqualTo("System-800W");
        Assertions.assertThat(systems.get(4).getCurrentProduction()).isCloseTo(800.f, within(0.1f));
    }

    @Test
    public void testSystemsWithoutValuesPlacedLast() throws Exception {
        // Test that systems without values for the sort field are placed at the end,
        // sorted by name

        // ARRANGE: Create test data
        User owner = addUser(true, "noValueTestOwner");
        Tag solarTag = addTag("No Value Test", "#00FFFF");
        String jwt = signIn("noValueTestOwner");

        // Create 3 systems WITH production values
        SolarSystem sys1 = createSystemWithTag(owner, solarTag, "HasValue-100W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys1.setCurrentValues(CurrentValues.builder()
                .inputWatt(100.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys1);

        SolarSystem sys2 = createSystemWithTag(owner, solarTag, "HasValue-300W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys2.setCurrentValues(CurrentValues.builder()
                .inputWatt(300.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys2);

        SolarSystem sys3 = createSystemWithTag(owner, solarTag, "HasValue-200W",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys3.setCurrentValues(CurrentValues.builder()
                .inputWatt(200.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys3);

        // Create 3 systems WITHOUT production values (0W = no value for currentproduction)
        SolarSystem sys4 = createSystemWithTag(owner, solarTag, "NoValue-Zulu",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys4.setCurrentValues(CurrentValues.builder()
                .inputWatt(0.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys4);

        SolarSystem sys5 = createSystemWithTag(owner, solarTag, "NoValue-Alpha",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys5.setCurrentValues(CurrentValues.builder()
                .inputWatt(0.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys5);

        SolarSystem sys6 = createSystemWithTag(owner, solarTag, "NoValue-Mike",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sys6.setCurrentValues(CurrentValues.builder()
                .inputWatt(0.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sys6);

        // ACT: Call aggregation endpoint sorted by current production
        var response = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // ASSERT: Verify results
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        List<SystemContributionDTO> systems = dto.getSystems().getContent();
        Assertions.assertThat(systems).hasSize(6);

        // First 3 systems should have values, sorted by production (ascending)
        Assertions.assertThat(systems.get(0).getName()).isEqualTo("HasValue-100W");
        Assertions.assertThat(systems.get(0).getCurrentProduction()).isCloseTo(100.f, within(0.1f));

        Assertions.assertThat(systems.get(1).getName()).isEqualTo("HasValue-200W");
        Assertions.assertThat(systems.get(1).getCurrentProduction()).isCloseTo(200.f, within(0.1f));

        Assertions.assertThat(systems.get(2).getName()).isEqualTo("HasValue-300W");
        Assertions.assertThat(systems.get(2).getCurrentProduction()).isCloseTo(300.f, within(0.1f));

        // Last 3 systems should have no values, sorted by name alphabetically
        Assertions.assertThat(systems.get(3).getName()).isEqualTo("NoValue-Alpha");
        Assertions.assertThat(systems.get(3).getCurrentProduction()).isCloseTo(0.f, within(0.1f));

        Assertions.assertThat(systems.get(4).getName()).isEqualTo("NoValue-Mike");
        Assertions.assertThat(systems.get(4).getCurrentProduction()).isCloseTo(0.f, within(0.1f));

        Assertions.assertThat(systems.get(5).getName()).isEqualTo("NoValue-Zulu");
        Assertions.assertThat(systems.get(5).getCurrentProduction()).isCloseTo(0.f, within(0.1f));
    }

    @Test
    public void testEfficiencySortingCalculation() throws Exception {
        // Test that efficiency sorting correctly calculates currentProduction / maxInstalledSolarPower

        // ARRANGE: Create test data
        User owner = addUser(true, "efficiencyTestOwner");
        Tag solarTag = addTag("Efficiency Test", "#FFFF00");
        String jwt = signIn("efficiencyTestOwner");

        // System A: 1000W production, 2000W max → 50% efficiency
        SolarSystem sysA = createSystemWithTag(owner, solarTag, "System-50pct",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sysA.getSystemInformations().setMaxInstalledSolarPower(2000.f);
        sysA.setCurrentValues(CurrentValues.builder()
                .inputWatt(1000.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sysA);

        // System B: 1500W production, 2000W max → 75% efficiency
        SolarSystem sysB = createSystemWithTag(owner, solarTag, "System-75pct",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sysB.getSystemInformations().setMaxInstalledSolarPower(2000.f);
        sysB.setCurrentValues(CurrentValues.builder()
                .inputWatt(1500.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sysB);

        // System C: 500W production, 2000W max → 25% efficiency
        SolarSystem sysC = createSystemWithTag(owner, solarTag, "System-25pct",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sysC.getSystemInformations().setMaxInstalledSolarPower(2000.f);
        sysC.setCurrentValues(CurrentValues.builder()
                .inputWatt(500.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sysC);

        // System D: 0W production, 2000W max → 0% efficiency (HAS value since maxPower > 0)
        SolarSystem sysD = createSystemWithTag(owner, solarTag, "System-0pct",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sysD.getSystemInformations().setMaxInstalledSolarPower(2000.f);
        sysD.setCurrentValues(CurrentValues.builder()
                .inputWatt(0.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sysD);

        // System E: 1000W production, null max → can't calculate efficiency (NO value since maxPower is null)
        SolarSystem sysE = createSystemWithTag(owner, solarTag, "System-NoMaxPower",
                PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        sysE.getSystemInformations().setMaxInstalledSolarPower(null);
        sysE.setCurrentValues(CurrentValues.builder()
                .inputWatt(1000.f)
                .lastSet(System.currentTimeMillis())
                .build());
        solarSystemRepository.save(sysE);

        // ACT: Call aggregation endpoint sorted by efficiency ascending
        var response = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?sortBy=efficiency&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        // ASSERT: Verify results
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        List<SystemContributionDTO> systems = dto.getSystems().getContent();
        Assertions.assertThat(systems).hasSize(5);

        // First 4 systems should have efficiency values, sorted ascending (0%, 25%, 50%, 75%)
        Assertions.assertThat(systems.get(0).getName()).isEqualTo("System-0pct");
        Assertions.assertThat(systems.get(0).getCurrentProduction()).isCloseTo(0.f, within(0.1f));
        Assertions.assertThat(systems.get(0).getMaxInstalledSolarPower()).isCloseTo(2000.f, within(0.1f));

        Assertions.assertThat(systems.get(1).getName()).isEqualTo("System-25pct");
        Assertions.assertThat(systems.get(1).getCurrentProduction()).isCloseTo(500.f, within(0.1f));
        Assertions.assertThat(systems.get(1).getMaxInstalledSolarPower()).isCloseTo(2000.f, within(0.1f));

        Assertions.assertThat(systems.get(2).getName()).isEqualTo("System-50pct");
        Assertions.assertThat(systems.get(2).getCurrentProduction()).isCloseTo(1000.f, within(0.1f));
        Assertions.assertThat(systems.get(2).getMaxInstalledSolarPower()).isCloseTo(2000.f, within(0.1f));

        Assertions.assertThat(systems.get(3).getName()).isEqualTo("System-75pct");
        Assertions.assertThat(systems.get(3).getCurrentProduction()).isCloseTo(1500.f, within(0.1f));
        Assertions.assertThat(systems.get(3).getMaxInstalledSolarPower()).isCloseTo(2000.f, within(0.1f));

        // Last system has no maxPower so no efficiency value, placed at end
        Assertions.assertThat(systems.get(4).getName()).isEqualTo("System-NoMaxPower");
    }

    @Test
    public void testPaginationWithSorting() throws Exception {
        // Test that pagination applies correctly after sorting

        // ARRANGE: Create 20 systems with distinct production values
        User owner = addUser(true, "paginationTestOwner");
        Tag solarTag = addTag("Pagination Test", "#AABBCC");
        String jwt = signIn("paginationTestOwner");

        // Create systems with production values from 100W to 2000W (100W increments)
        for (int i = 1; i <= 20; i++) {
            float production = i * 100.f;
            SolarSystem sys = createSystemWithTag(owner, solarTag, "System-" + String.format("%04d", (int)production) + "W",
                    PublicMode.ALL, SolarSystemType.GRID_BATTERY);
            sys.setCurrentValues(CurrentValues.builder()
                    .inputWatt(production)
                    .lastSet(System.currentTimeMillis())
                    .build());
            solarSystemRepository.save(sys);
        }

        // ACT & ASSERT: Request page 0 (first 5 systems, lowest production)
        var responsePage0 = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?page=0&size=5&sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(responsePage0.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dtoPage0 = objectMapper.readValue(responsePage0.getBody(), TagAggregationDTO.class);

        Assertions.assertThat(dtoPage0.getSystems().getPage()).isEqualTo(0);
        Assertions.assertThat(dtoPage0.getSystems().getSize()).isEqualTo(5);
        Assertions.assertThat(dtoPage0.getSystems().getTotalElements()).isEqualTo(20);
        Assertions.assertThat(dtoPage0.getSystems().getTotalPages()).isEqualTo(4);
        Assertions.assertThat(dtoPage0.getSystems().getContent()).hasSize(5);

        // Page 0 should contain systems ranked 1-5 (100W, 200W, 300W, 400W, 500W)
        List<SystemContributionDTO> page0Systems = dtoPage0.getSystems().getContent();
        Assertions.assertThat(page0Systems.get(0).getName()).isEqualTo("System-0100W");
        Assertions.assertThat(page0Systems.get(0).getCurrentProduction()).isCloseTo(100.f, within(0.1f));
        Assertions.assertThat(page0Systems.get(1).getName()).isEqualTo("System-0200W");
        Assertions.assertThat(page0Systems.get(1).getCurrentProduction()).isCloseTo(200.f, within(0.1f));
        Assertions.assertThat(page0Systems.get(2).getName()).isEqualTo("System-0300W");
        Assertions.assertThat(page0Systems.get(2).getCurrentProduction()).isCloseTo(300.f, within(0.1f));
        Assertions.assertThat(page0Systems.get(3).getName()).isEqualTo("System-0400W");
        Assertions.assertThat(page0Systems.get(3).getCurrentProduction()).isCloseTo(400.f, within(0.1f));
        Assertions.assertThat(page0Systems.get(4).getName()).isEqualTo("System-0500W");
        Assertions.assertThat(page0Systems.get(4).getCurrentProduction()).isCloseTo(500.f, within(0.1f));

        // ACT & ASSERT: Request page 1 (systems ranked 6-10)
        var responsePage1 = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?page=1&size=5&sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(responsePage1.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dtoPage1 = objectMapper.readValue(responsePage1.getBody(), TagAggregationDTO.class);

        Assertions.assertThat(dtoPage1.getSystems().getPage()).isEqualTo(1);
        Assertions.assertThat(dtoPage1.getSystems().getContent()).hasSize(5);

        // Page 1 should contain systems ranked 6-10 (600W, 700W, 800W, 900W, 1000W)
        List<SystemContributionDTO> page1Systems = dtoPage1.getSystems().getContent();
        Assertions.assertThat(page1Systems.get(0).getName()).isEqualTo("System-0600W");
        Assertions.assertThat(page1Systems.get(0).getCurrentProduction()).isCloseTo(600.f, within(0.1f));
        Assertions.assertThat(page1Systems.get(1).getName()).isEqualTo("System-0700W");
        Assertions.assertThat(page1Systems.get(1).getCurrentProduction()).isCloseTo(700.f, within(0.1f));
        Assertions.assertThat(page1Systems.get(2).getName()).isEqualTo("System-0800W");
        Assertions.assertThat(page1Systems.get(2).getCurrentProduction()).isCloseTo(800.f, within(0.1f));
        Assertions.assertThat(page1Systems.get(3).getName()).isEqualTo("System-0900W");
        Assertions.assertThat(page1Systems.get(3).getCurrentProduction()).isCloseTo(900.f, within(0.1f));
        Assertions.assertThat(page1Systems.get(4).getName()).isEqualTo("System-1000W");
        Assertions.assertThat(page1Systems.get(4).getCurrentProduction()).isCloseTo(1000.f, within(0.1f));

        // ACT & ASSERT: Request page 3 (last page, systems ranked 16-20)
        var responsePage3 = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?page=3&size=5&sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(responsePage3.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dtoPage3 = objectMapper.readValue(responsePage3.getBody(), TagAggregationDTO.class);

        Assertions.assertThat(dtoPage3.getSystems().getPage()).isEqualTo(3);
        Assertions.assertThat(dtoPage3.getSystems().getContent()).hasSize(5);

        // Page 3 should contain systems ranked 16-20 (1600W, 1700W, 1800W, 1900W, 2000W)
        List<SystemContributionDTO> page3Systems = dtoPage3.getSystems().getContent();
        Assertions.assertThat(page3Systems.get(0).getName()).isEqualTo("System-1600W");
        Assertions.assertThat(page3Systems.get(0).getCurrentProduction()).isCloseTo(1600.f, within(0.1f));
        Assertions.assertThat(page3Systems.get(1).getName()).isEqualTo("System-1700W");
        Assertions.assertThat(page3Systems.get(1).getCurrentProduction()).isCloseTo(1700.f, within(0.1f));
        Assertions.assertThat(page3Systems.get(2).getName()).isEqualTo("System-1800W");
        Assertions.assertThat(page3Systems.get(2).getCurrentProduction()).isCloseTo(1800.f, within(0.1f));
        Assertions.assertThat(page3Systems.get(3).getName()).isEqualTo("System-1900W");
        Assertions.assertThat(page3Systems.get(3).getCurrentProduction()).isCloseTo(1900.f, within(0.1f));
        Assertions.assertThat(page3Systems.get(4).getName()).isEqualTo("System-2000W");
        Assertions.assertThat(page3Systems.get(4).getCurrentProduction()).isCloseTo(2000.f, within(0.1f));

        // Verify no overlap: systems from page 0 should not appear in page 1 or page 3
        List<String> page0Names = page0Systems.stream().map(SystemContributionDTO::getName).toList();
        List<String> page1Names = page1Systems.stream().map(SystemContributionDTO::getName).toList();
        List<String> page3Names = page3Systems.stream().map(SystemContributionDTO::getName).toList();

        Assertions.assertThat(page1Names).doesNotContainAnyElementsOf(page0Names);
        Assertions.assertThat(page3Names).doesNotContainAnyElementsOf(page0Names);
        Assertions.assertThat(page3Names).doesNotContainAnyElementsOf(page1Names);

        // ACT & ASSERT: Test with different page size (size=10)
        var responsePage0Size10 = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?page=0&size=10&sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(responsePage0Size10.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dtoPage0Size10 = objectMapper.readValue(responsePage0Size10.getBody(), TagAggregationDTO.class);

        Assertions.assertThat(dtoPage0Size10.getSystems().getPage()).isEqualTo(0);
        Assertions.assertThat(dtoPage0Size10.getSystems().getSize()).isEqualTo(10);
        Assertions.assertThat(dtoPage0Size10.getSystems().getTotalElements()).isEqualTo(20);
        Assertions.assertThat(dtoPage0Size10.getSystems().getTotalPages()).isEqualTo(2);  // 20 systems / 10 per page = 2 pages
        Assertions.assertThat(dtoPage0Size10.getSystems().getContent()).hasSize(10);

        // Page 0 with size=10 should contain systems 1-10 (100W to 1000W)
        List<SystemContributionDTO> page0Size10Systems = dtoPage0Size10.getSystems().getContent();
        Assertions.assertThat(page0Size10Systems.get(0).getName()).isEqualTo("System-0100W");
        Assertions.assertThat(page0Size10Systems.get(0).getCurrentProduction()).isCloseTo(100.f, within(0.1f));
        Assertions.assertThat(page0Size10Systems.get(9).getName()).isEqualTo("System-1000W");
        Assertions.assertThat(page0Size10Systems.get(9).getCurrentProduction()).isCloseTo(1000.f, within(0.1f));

        // Request page 1 with size=10 (should contain systems 11-20)
        var responsePage1Size10 = doRestRequest(
                "/api/tags/aggregation/" + solarTag.getId() + "?page=1&size=10&sortBy=currentproduction&sortOrder=asc",
                null,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(responsePage1Size10.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dtoPage1Size10 = objectMapper.readValue(responsePage1Size10.getBody(), TagAggregationDTO.class);

        Assertions.assertThat(dtoPage1Size10.getSystems().getPage()).isEqualTo(1);
        Assertions.assertThat(dtoPage1Size10.getSystems().getSize()).isEqualTo(10);
        Assertions.assertThat(dtoPage1Size10.getSystems().getContent()).hasSize(10);

        // Page 1 with size=10 should contain systems 11-20 (1100W to 2000W)
        List<SystemContributionDTO> page1Size10Systems = dtoPage1Size10.getSystems().getContent();
        Assertions.assertThat(page1Size10Systems.get(0).getName()).isEqualTo("System-1100W");
        Assertions.assertThat(page1Size10Systems.get(0).getCurrentProduction()).isCloseTo(1100.f, within(0.1f));
        Assertions.assertThat(page1Size10Systems.get(9).getName()).isEqualTo("System-2000W");
        Assertions.assertThat(page1Size10Systems.get(9).getCurrentProduction()).isCloseTo(2000.f, within(0.1f));

        // Verify no overlap between size=10 pages
        List<String> page0Size10Names = page0Size10Systems.stream().map(SystemContributionDTO::getName).toList();
        List<String> page1Size10Names = page1Size10Systems.stream().map(SystemContributionDTO::getName).toList();
        Assertions.assertThat(page1Size10Names).doesNotContainAnyElementsOf(page0Size10Names);
    }
}
