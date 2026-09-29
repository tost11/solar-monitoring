package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.dto.SystemContributionDTO;
import de.tostsoft.solarmonitoring.lib.model.DayValues;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for caching tag aggregation data.
 * Extracts the expensive data-fetching logic into a separate service
 * so Spring AOP can properly cache it (avoiding self-invocation issues).
 */
@Service
public class TagAggregationCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(TagAggregationCacheService.class);

    @Autowired
    private TagService tagService;

    @Autowired
    private InfluxService influxService;

    @Autowired
    private UserService userService;

    @Data
    @Builder
    public static class AggregationData {
        private Tag tag;
        private List<SystemContributionDTO> systems;
        private int onlineCount;
        private float totalDayProducedKWH;
        private float totalDayConsumedKWH;
        private float totalCurrentProduction;
        private float totalCurrentConsumption;
        private float totalCurrentGrid;
        private float totalBatteryRemainingKWH;
    }

    /**
     * Fetches and aggregates tag data. Cached for public users only.
     * Authenticated users bypass the cache to get fresh data.
     */
    //IMPORTATNT this here needs to be in a separate service and public to be working with cache anotation
    @Cacheable(
        value = "tagAggregationPublic",
        key = "#tagId",
        unless = "@userService.getLoggedInUserLazyLoadedNoException() != null"
    )
    public AggregationData getAggregationData(String tagId) {
        User user = userService.getLoggedInUserFullNoException();

        Pair<Tag, List<Pair<SolarSystem, PublicMode>>> tagAndSystems =
            tagService.findTagWithAccessibleSystems(tagId);

        Tag tag = tagAndSystems.getLeft();
        List<Pair<SolarSystem, PublicMode>> accessibleSystems = tagAndSystems.getRight();

        if (accessibleSystems.isEmpty()) {
            return AggregationData.builder()
                .tag(tag)
                .systems(new ArrayList<>())
                .onlineCount(0)
                .totalDayProducedKWH(0)
                .totalDayConsumedKWH(0)
                .totalCurrentProduction(0)
                .totalCurrentConsumption(0)
                .totalCurrentGrid(0)
                .totalBatteryRemainingKWH(0)
                .build();
        }

        float totalDayProducedKWH = 0;
        float totalDayConsumedKWH = 0;
        float totalCurrentProduction = 0;
        float totalCurrentConsumption = 0;
        float totalCurrentGrid = 0;
        float totalBatteryRemainingKWH = 0;
        int onlineCount = 0;

        List<SystemContributionDTO> contributionDTOs = new ArrayList<>();

        for (Pair<SolarSystem, PublicMode> pair : accessibleSystems) {
            SolarSystem system = pair.getLeft();
            PublicMode publicMode = pair.getRight();

            boolean isOnline = system.isOnline();
            if (isOnline) {
                onlineCount++;
            }

            boolean showConsumption = publicMode == null || publicMode == PublicMode.ALL;
            String role = publicMode == null ? "ADMIN" : "PUBLIC";

            if (publicMode == null && user != null) {
                boolean isOwner = StringUtils.equals(system.getOwnedBy().getId(), user.getId());
                if (!isOwner) {
                    var managesOpt = system.getManagedBy().stream()
                        .filter(m -> StringUtils.equals(m.getUser().getId(), user.getId()))
                        .findFirst();
                    if (managesOpt.isPresent()) {
                        Permissions permission = managesOpt.get().getPermission();
                        role = permission.name();
                    }
                }
            }

            float dayProducedKWH = 0;
            Float dayConsumedKWH = null;
            Float dayConsumedBase = null;
            Float dayGridConsumed = null;
            Float dayGridFeedIn = null;

            ZoneId zoneId = ZoneId.of(system.getTimezone() == null ? "UTC" : system.getTimezone());
            LocalDate today = LocalDate.now(zoneId);

            DayValues dayValues = system.getDayValues();
            boolean useStoredDayValues = dayValues != null
                && dayValues.getLocalDate() != null
                && dayValues.getLocalDate().equals(today);

            if (useStoredDayValues) {
                dayProducedKWH = dayValues.getProducedKWH() != null ? dayValues.getProducedKWH() : 0;
                if (showConsumption) {
                    dayConsumedBase = dayValues.getConsumedKWH();
                    dayGridConsumed = dayValues.getGridConsumedKWH();
                    dayGridFeedIn = dayValues.getGridFeedInKWH();
                }
            } else {
                // Fallback to a per-system Influx query is disabled; missing or stale dayValues
                // now simply result in 0/null for this system.
                // Reason: the SOLAR_DAY_DATA measurement in Influx is only written by the updater,
                // so in every case where stored dayValues are missing or stale (midnight rollover,
                // system without data, updater outage) the query returned the same 0/null values.
                // The only case where it returned a different value was the deployment window
                // before dayValues was backfilled, which is covered by a one-time
                // needsStatisticRecalculation backfill on all systems. Keeping the slow
                // N-queries request path alive for that case was not worth it.
                // Commented out instead of deleted so it can be re-enabled quickly if needed.
                /*
                ZonedDateTime startOfToday = today.atStartOfDay(zoneId);
                ZonedDateTime endOfToday = today.plusDays(1).atStartOfDay(zoneId).minusSeconds(1);
                Date fromDate = Date.from(startOfToday.toInstant());
                Date toDate = Date.from(endOfToday.toInstant());

                try {
                    var fluxTables = influxService.getStatisticsDataAsJson(
                        system,
                        InfluxMeasurement.SOLAR_DAY_DATA,
                        fromDate,
                        toDate,
                        !showConsumption
                    );

                    if (fluxTables != null && !fluxTables.isEmpty()) {
                        for (var table : fluxTables) {
                            for (var record : table.getRecords()) {
                                String field = (String) record.getValueByKey("_field");
                                Object value = record.getValue();

                                if (value instanceof Number) {
                                    float floatValue = ((Number) value).floatValue();

                                    if (StringUtils.equals(field, InfluxService.API_NAMING_PRODUCED)) {
                                        dayProducedKWH = Math.max(dayProducedKWH, floatValue);
                                    } else if (showConsumption) {
                                        if (StringUtils.equals(field, InfluxService.API_NAMING_CONSUMED)) {
                                            dayConsumedBase = Math.max(dayConsumedBase != null ? dayConsumedBase : 0, floatValue);
                                        } else if (StringUtils.equals(field, InfluxService.API_NAMING_GRID_CONSUMPTION)) {
                                            dayGridConsumed = Math.max(dayGridConsumed != null ? dayGridConsumed : 0, floatValue);
                                        } else if (StringUtils.equals(field, InfluxService.API_NAMING_GRID_FEEDIN)) {
                                            dayGridFeedIn = Math.max(dayGridFeedIn != null ? dayGridFeedIn : 0, floatValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error fetching statistics data for system {} in tag aggregation", system.getId(), e);
                }
                */
            }

            boolean showGridInfo = system.getViewData() != null
                && system.getViewData().getShowGridInfo() != null
                && system.getViewData().getShowGridInfo();

            if (showGridInfo && dayConsumedBase != null) {
                float totalConsumption = dayConsumedBase;
                if (dayGridConsumed != null) {
                    totalConsumption += dayGridConsumed;
                }
                if (dayGridFeedIn != null) {
                    totalConsumption -= dayGridFeedIn;
                }
                dayConsumedKWH = Math.max(0, totalConsumption);
            } else if (dayConsumedBase != null) {
                dayConsumedKWH = dayConsumedBase;
            }

            float currentProduction = 0;
            Float currentConsumption = null;
            Float currentGrid = null;
            Float batteryPercentage = null;
            Float batteryRemainingKWH = null;

            if (system.getCurrentValues() != null && system.isOnline(Duration.of(15, ChronoUnit.MINUTES))) {
                currentProduction = system.getCurrentValues().getInputWatt() != null
                    ? system.getCurrentValues().getInputWatt() : 0;

                if (showConsumption) {
                    Float outputWatt = system.getCurrentValues().getOutputWatt();
                    Float gridWatt = system.getCurrentValues().getGridWatt();

                    if (showGridInfo && (gridWatt != null || outputWatt != null)) {
                        currentConsumption = Math.max(0, (outputWatt == null ? 0 : outputWatt) + (gridWatt == null ? 0 : gridWatt));
                    } else if (outputWatt != null) {
                        currentConsumption = outputWatt;
                    }

                    currentGrid = gridWatt;

                    batteryPercentage = system.getCurrentValues().getBatteryPercentage();
                    Float batteryCapacity = system.getSystemInformations() != null
                        ? system.getSystemInformations().getBatteryCapacity() : null;
                    if (batteryPercentage != null && batteryCapacity != null) {
                        batteryRemainingKWH = batteryCapacity * batteryPercentage / 100f;
                    }
                }
            }

            totalDayProducedKWH += dayProducedKWH;
            if (dayConsumedKWH != null) {
                totalDayConsumedKWH += dayConsumedKWH;
            }
            if (isOnline) {
                totalCurrentProduction += currentProduction;
                if (currentConsumption != null) {
                    totalCurrentConsumption += currentConsumption;
                }
                if (currentGrid != null) {
                    totalCurrentGrid += currentGrid;
                }
                if (batteryRemainingKWH != null) {
                    totalBatteryRemainingKWH += batteryRemainingKWH;
                }
            }

            contributionDTOs.add(SystemContributionDTO.builder()
                .id(system.getId())
                //if system only accessible by public and public name set use that, instead use real name
                .name(publicMode != null && system.getSystemInformations().getPublicName() != null ? system.getSystemInformations().getPublicName() : system.getSystemInformations().getViewName())
                .type(system.getType() != null ? system.getType().name() : "UNKNOWN")
                .isOnline(isOnline)
                .dayProducedKWH(dayProducedKWH)
                .dayConsumedKWH(dayConsumedKWH)
                .currentProduction(currentProduction)
                .currentConsumption(currentConsumption)
                .currentGrid(currentGrid)
                .batteryPercentage(batteryPercentage)
                .batteryRemainingKWH(batteryRemainingKWH)
                .role(role)
                .maxInstalledSolarPower(system.getSystemInformations().getMaxInstalledSolarPower())
                .build());
        }

        return AggregationData.builder()
            .tag(tag)
            .systems(contributionDTOs)
            .onlineCount(onlineCount)
            .totalDayProducedKWH(totalDayProducedKWH)
            .totalDayConsumedKWH(totalDayConsumedKWH)
            .totalCurrentProduction(totalCurrentProduction)
            .totalCurrentConsumption(totalCurrentConsumption)
            .totalCurrentGrid(totalCurrentGrid)
            .totalBatteryRemainingKWH(totalBatteryRemainingKWH)
            .build();
    }
}
