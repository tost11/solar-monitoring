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

        // Check aggregated current totals
        Assertions.assertThat(dto.getTotalCurrentProduction()).isEqualTo(9000.f);  // 5000 + 3000 + 1000
        Assertions.assertThat(dto.getTotalCurrentConsumption()).isEqualTo(6500.f); // 2000 + 4000 + 500
        Assertions.assertThat(dto.getTotalCurrentGrid()).isCloseTo(-2000.f, within(1.f)); // -3000 + 1000

        // Check per-system contributions
        Assertions.assertThat(dto.getSystems()).hasSize(3);

        SystemContributionDTO sys1 = dto.getSystems().stream()
            .filter(s -> s.getName().equals("System1"))
            .findFirst()
            .orElseThrow();

        // System 1 assertions
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(15.f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(10.f);
        Assertions.assertThat(sys1.getDayProductionPercentage()).isCloseTo(50.f, within(0.1f)); // 15/30
        Assertions.assertThat(sys1.getDayConsumptionPercentage()).isCloseTo(41.7f, within(0.2f)); // 10/24
        Assertions.assertThat(sys1.getCurrentProduction()).isEqualTo(5000.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(2000.f);
        Assertions.assertThat(sys1.getCurrentProductionPercentage()).isCloseTo(55.6f, within(0.1f)); // 5000/9000
        Assertions.assertThat(sys1.getCurrentConsumptionPercentage()).isCloseTo(30.8f, within(0.1f)); // 2000/6500
        Assertions.assertThat(sys1.isOnline()).isTrue();

        // System 3 (offline) assertions
        SystemContributionDTO sys3 = dto.getSystems().stream()
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
        Assertions.assertThat(dto.getSystems()).isEmpty();
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
        for (SystemContributionDTO sys : dto.getSystems()) {
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
        SystemContributionDTO sys1 = dto.getSystems().stream()
            .filter(s -> s.getName().equals("AllDataSystem"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(8.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(1500.f);
        Assertions.assertThat(sys1.getCurrentGrid()).isEqualTo(-500.f);

        // Verify system 2 (PRODUCTION) has NO consumption
        SystemContributionDTO sys2 = dto.getSystems().stream()
            .filter(s -> s.getName().equals("ProductionOnlySystem"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys2.getDayProducedKWH()).isEqualTo(15.f);
        Assertions.assertThat(sys2.getDayConsumedKWH()).isNull();
        Assertions.assertThat(sys2.getCurrentConsumption()).isNull();
        Assertions.assertThat(sys2.getCurrentGrid()).isNull();

        // Verify system 3 (ALL) has consumption
        SystemContributionDTO sys3 = dto.getSystems().stream()
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
        SystemContributionDTO sys1 = dto.getSystems().stream()
            .filter(s -> s.getName().equals("PublicSystem1"))
            .findFirst()
            .orElseThrow();
        Assertions.assertThat(sys1.getRole()).isEqualTo("PUBLIC");
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(12.f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(9.f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(1800.f);

        // Verify system 2 (PRODUCTION) has NO consumption and PUBLIC role
        SystemContributionDTO sys2 = dto.getSystems().stream()
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
        Assertions.assertThat(dto.getSystems()).isEmpty();
    }
}
