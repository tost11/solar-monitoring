package de.tostsoft.solarmonitoring.app.service;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxFields;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement.SELDOM_CHANGING_STATS;

@Service
public class InfluxService {

    public static final String API_NAMING_PRODUCED = "Produced";
    public static final String API_NAMING_CONSUMED = "Consumed";
    public static final String API_NAMING_BATTERY = "Battery";
    public static final String API_NAMING_DIFFERENCE = "Difference";
    public static final String API_NAMING_GRID_FEEDIN = "GridFeedIn";
    public static final String API_NAMING_GRID_CONSUMPTION = "GridConsumed";

    private static final Logger LOG = LoggerFactory.getLogger(InfluxService.class);

    @Autowired
    private InfluxConnection influxConnection;

    private final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    static private final int NUM_TIME_STAMPS = 60;

    /*public List<FluxTable> getStatisticsDataAsJson(SolarSystem solarSystem,InfluxMeasurement measurement,Date from ,Date to,boolean onlyProduction) {

        var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());

        var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
        var instantTo = ZonedDateTime.ofInstant(to.toInstant(), zId);

        String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcProdKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesProdKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.prodKWHField + "\")" +
                    "\n";
        }else{
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcConsKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesConsKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcProdKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesProdKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcBatteryKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesBatteryKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.prodKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.consKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxFields.batteryKWHField + "\"\n" +
                    ")\n";
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }*/

    public List<FluxTable> getStatisticsDataAsJson(SolarSystem solarSystem, InfluxMeasurement measurement, Date from, Date to, boolean onlyProduction) {
        var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
        var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
        var instantTo = ZonedDateTime.ofInstant(to.toInstant(), zId);

        String systemTag = solarSystem.getInfluxTagName();
        String bucket = solarSystem.getOwnedBy().getInfluxBucketName();

        Set<String> totalFilter = (solarSystem.getViewData() != null && solarSystem.getViewData().getTotalFilter() != null)
            ? solarSystem.getViewData().getTotalFilter()
            : Collections.emptySet();

        StringBuilder fieldFilterBuilder = new StringBuilder();
        boolean firstCondition = true;

        if (!totalFilter.contains(InfluxFields.prodKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.prodKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcProdKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcProdKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesProdKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesProdKWHField).append("\"");
            firstCondition = false;
        }

        if (!totalFilter.contains(InfluxFields.consKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.consKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcConsKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcConsKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesConsKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesConsKWHField).append("\"");
            firstCondition = false;
        }

        if (!totalFilter.contains(InfluxFields.batteryKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.batteryKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcBatteryKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcBatteryKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesBatteryKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesBatteryKWHField).append("\"");
            firstCondition = false;
        }

        if (!totalFilter.contains(InfluxFields.gridConsKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.gridConsKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcGridConsKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcGridConsKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesGridConsKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesGridConsKWHField).append("\"");
            firstCondition = false;
        }

        if (!totalFilter.contains(InfluxFields.gridFeedInKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.gridFeedInKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcGridFeedInKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcGridFeedInKWHField).append("\"");
            firstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesGridFeedInKWHField.getName())) {
            if (!firstCondition) fieldFilterBuilder.append(" or\n");
            fieldFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesGridFeedInKWHField).append("\"");
            firstCondition = false;
        }

        String fieldFilter = fieldFilterBuilder.toString();

        StringBuilder prodOnlyFilterBuilder = new StringBuilder();
        boolean prodFirstCondition = true;

        if (!totalFilter.contains(InfluxFields.prodKWHField.getName())) {
            if (!prodFirstCondition) prodOnlyFilterBuilder.append(" or\n");
            prodOnlyFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.prodKWHField).append("\"");
            prodFirstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcProdKWHField.getName())) {
            if (!prodFirstCondition) prodOnlyFilterBuilder.append(" or\n");
            prodOnlyFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcProdKWHField).append("\"");
            prodFirstCondition = false;
        }
        if (!totalFilter.contains(InfluxFields.calcByDevicesProdKWHField.getName())) {
            if (!prodFirstCondition) prodOnlyFilterBuilder.append(" or\n");
            prodOnlyFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesProdKWHField).append("\"");
            prodFirstCondition = false;
        }

        String prodOnlyFieldFilter = prodOnlyFilterBuilder.toString();

        StringBuilder base = new StringBuilder(1024);
        base.append("base = from(bucket: \"").append(bucket).append("\")\n")
            .append("  |> range(start: ").append(zoneFormatter.format(instantFrom)).append(", stop: ").append(zoneFormatter.format(instantTo)).append(")\n")
            .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(measurement).append("\")\n")
            .append("  |> filter(fn: (r) => r.system == \"").append(systemTag).append("\")\n")
            .append("  |> filter(fn: (r) =>\n")
            .append(fieldFilter).append("\n")
            .append("  )\n")
            .append("  |> drop(columns: [\"type\"]\n)")
            .append("  |> pivot(rowKey: [\"_time\", \"system\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");

        StringBuilder baseOnlyProduction = new StringBuilder(1024);
        baseOnlyProduction.append("base = from(bucket: \"").append(bucket).append("\")\n")
            .append("  |> range(start: ").append(zoneFormatter.format(instantFrom)).append(", stop: ").append(zoneFormatter.format(instantTo)).append(")\n")
            .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(measurement).append("\")\n")
            .append("  |> filter(fn: (r) => r.system == \"").append(systemTag).append("\")\n")
            .append("  |> filter(fn: (r) =>\n")
            .append(prodOnlyFieldFilter).append("\n")
            .append("  )\n")
            .append("  |> drop(columns: [\"type\"]\n)")
            .append("  |> pivot(rowKey: [\"_time\", \"system\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");

            StringBuilder produced = new StringBuilder(512);
            produced.append("produced = base\n")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("    _time: r._time,\n")
                        .append("    system: r.system,\n")
                        .append("    _measurement: r._measurement,\n")
                        .append("    id: r.id,\n")
                        .append("    _field: \"").append(API_NAMING_PRODUCED).append("\",\n")
                        .append("    _value: if exists r.").append(InfluxFields.prodKWHField).append(" then r.").append(InfluxFields.prodKWHField)
                        .append(" else if exists r.").append(InfluxFields.calcByDevicesProdKWHField).append(" then r.").append(InfluxFields.calcByDevicesProdKWHField)
                        .append(" else r.").append(InfluxFields.calcProdKWHField).append("\n")
                        .append("  }))\n");

        StringBuilder consumed = new StringBuilder(512);
        consumed.append("consumed = base\n")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("    _time: r._time,\n")
                        .append("    system: r.system,\n")
                        .append("    _measurement: r._measurement,\n")
                        .append("    id: r.id,\n")
                        .append("    _field: \"").append(API_NAMING_CONSUMED).append("\",\n")
                        .append("    _value: if exists r.").append(InfluxFields.consKWHField).append(" then r.").append(InfluxFields.consKWHField)
                        .append(" else if exists r.").append(InfluxFields.calcByDevicesConsKWHField).append(" then r.").append(InfluxFields.calcByDevicesConsKWHField)
                        .append(" else r.").append(InfluxFields.calcConsKWHField).append("\n")
                        .append("  }))\n");

        StringBuilder battery = new StringBuilder(512);
        battery.append("battery = base\n")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("    _time: r._time,\n")
                        .append("    system: r.system,\n")
                        .append("    _measurement: r._measurement,\n")
                        .append("    id: r.id,\n")
                        .append("    _field: \"").append(API_NAMING_BATTERY).append("\",\n")
                        .append("    _value: if exists r.").append(InfluxFields.batteryKWHField).append(" then r.").append(InfluxFields.batteryKWHField)
                        .append(" else if exists r.").append(InfluxFields.calcByDevicesBatteryKWHField).append(" then r.").append(InfluxFields.calcByDevicesBatteryKWHField)
                        .append(" else r.").append(InfluxFields.calcBatteryKWHField).append("\n")
                        .append("  }))\n");

        StringBuilder gridFeedIn = new StringBuilder(512);
        gridFeedIn.append("gridFeedIn = base\n")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("    _time: r._time,\n")
                        .append("    system: r.system,\n")
                        .append("    _measurement: r._measurement,\n")
                        .append("    id: r.id,\n")
                        .append("    _field: \"").append(API_NAMING_GRID_FEEDIN).append("\",\n")
                        .append("    _value: if exists r.").append(InfluxFields.gridFeedInKWHField).append(" then r.").append(InfluxFields.gridFeedInKWHField)
                        .append(" else if exists r.").append(InfluxFields.calcByDevicesGridFeedInKWHField).append(" then r.").append(InfluxFields.calcByDevicesGridFeedInKWHField)
                        .append(" else r.").append(InfluxFields.calcGridFeedInKWHField).append("\n")
                        .append("  }))\n");

        StringBuilder gridConsumed = new StringBuilder(512);
        gridConsumed.append("gridConsumed = base\n")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("    _time: r._time,\n")
                        .append("    system: r.system,\n")
                        .append("    _measurement: r._measurement,\n")
                        .append("    id: r.id,\n")
                        .append("    _field: \"").append(API_NAMING_GRID_CONSUMPTION).append("\",\n")
                        .append("    _value: if exists r.").append(InfluxFields.gridConsKWHField).append(" then r.").append(InfluxFields.gridConsKWHField)
                        .append(" else if exists r.").append(InfluxFields.calcByDevicesGridConsKWHField).append(" then r.").append(InfluxFields.calcByDevicesGridConsKWHField)
                        .append(" else r.").append(InfluxFields.calcGridConsKWHField).append("\n")
                        .append("  }))\n");

        StringBuilder query = new StringBuilder(4096);

        if (onlyProduction) {
            query.append(baseOnlyProduction).append("\n").append(produced)
                    .append("  |> keep(columns: [\"_time\", \"system\", \"_field\", \"_value\",\"id\",\"_measurement\"])\n")
                    .append("  |> sort(columns: [\"_time\"])\n\n")
                    .append("produced");
        } else {
            query.append(base).append("\n").append(produced).append("\n").append(consumed).append("\n").append(battery).append("\n").append(gridFeedIn).append("\n").append(gridConsumed).append("\n")
                    .append("union(tables: [produced, consumed, battery, gridFeedIn, gridConsumed])\n")
                    .append("  |> keep(columns: [\"_time\", \"system\", \"_field\", \"_value\",\"id\",\"_measurement\"])\n")
                    .append("  |> sort(columns: [\"_time\", \"_field\"])\n");
        }

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getlastTwoDaysStatistic(SolarSystem solarSystem,InfluxMeasurement measurement,boolean onlyProduction) {

        Instant now = Instant.now();
        Instant twoDayAgo = now.minus(2, ChronoUnit.DAYS);

        return getStatisticsDataAsJson(solarSystem,measurement,new Date(twoDayAgo.toEpochMilli()),new Date(now.toEpochMilli()),onlyProduction);
    }

    public String generatePublicQueryParameters(){
        StringBuilder query = new StringBuilder(256);
        query.append(" and (\n")
                .append("      r[\"_field\"] == \"InputWattDC\" or\n")
                .append("      r[\"_field\"] == \"InputVoltageDC\" or\n")
                .append("      r[\"_field\"] == \"InputAmpereDC\")");
        return query.toString();
    }

    public List<FluxTable> getAllDataAsJson(SolarSystem solarSystem,Date from, Date to,boolean onlyProduction) {

        Instant instantFrom = from.toInstant();
        Instant instantToday = to.toInstant();
        long sec = Duration.between(instantFrom,instantToday).getSeconds();
        sec = sec / 60;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }
        StringBuilder query = new StringBuilder(1024);
        if (onlyProduction) {
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(instantFrom).append(", stop: ").append(instantToday).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) =>\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\"").append(generatePublicQueryParameters()).append(") or\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_DEVICE).append("\"").append(generatePublicQueryParameters()).append(") or\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_DC).append("\"))\n")
                    .append("  |> drop(columns: [\"type\"]\n)")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )")
                    .append("\n");
        }else {
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(instantFrom).append(", stop: ").append(instantToday).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) => \n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_DEVICE).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_DC).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_AC).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_BATTERY).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_OUTPUT_DC).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_OUTPUT_AC).append("\" or\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_GRID).append("\")\n")
                    .append("  |> drop(columns: [\"type\"]\n)")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )")
                    .append("\n");
        }
        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getLastDayDataAsJson(SolarSystem solarSystem) {

        Instant instantFrom = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant instantToday = Instant.now();
        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(instantFrom).append(", stop: ").append(instantToday).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) => (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DAY_DATA).append("\"))\n")
                    .append("  |> last()")
                    .append("\n");
        return influxConnection.getClient().getQueryApi().query(query.toString());
    }


    public List<FluxTable> getProductionCombined(List<? extends  Pair<SolarSystem,Boolean>> solarSystems, Date from, Date to, Map<String,Integer> systemMappings) {

        //todo fix when only one system is working

        Instant instantFrom = from.toInstant();
        Instant instantToday = to.toInstant();
        long sec = Duration.between(instantFrom,instantToday).getSeconds();
        sec = sec / 60;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }

        StringBuilder query = new StringBuilder(2048);

        for(int i=0;i<solarSystems.size();i++){
            var solarSystem = solarSystems.get(i).getKey();
            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query.append("d").append(i).append(" = ");
            }
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(instantFrom).append(", stop: ").append(instantToday).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) =>\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\") and\n")
                    .append("    (r[\"_field\"] == \"InputWatt\"))\n")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )")
                    .append("  |> drop(columns: [\"type\"]\n)")
                    .append("  |> map(fn: (r) => ({ _value:r._value, _time:r._time, _field:r._field+\"_").append(id).append("\" }))")
                    .append("\n\n");
        }

        if(solarSystems.size() > 1) {
            query.append("union(tables: [");

            var joiner = new StringJoiner(", ");

            for (int i = 0; i < solarSystems.size(); i++) {
                joiner.add("d" + i);
            }
            query.append(joiner.toString());

            query.append("])");
        }

        /*String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }else {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) => \n"+
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_AC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_BATTERY + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_DC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }*/

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getLastFiveMin(SolarSystem solarSystem, long duration,boolean onlyProduction) {

        Instant now = Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long sec = Duration.ofMillis(duration).getSeconds();
        sec = sec / NUM_TIME_STAMPS;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }

        now.plus((sec/2)-1,ChronoUnit.SECONDS);
        fiveMinAgo.minus((sec/2)-1,ChronoUnit.SECONDS);

        StringBuilder query = new StringBuilder(1024);
        if (onlyProduction) {
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(fiveMinAgo).append(", stop: ").append(now).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) =>\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\"").append(generatePublicQueryParameters()).append(") or\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_DEVICE).append("\"").append(generatePublicQueryParameters()).append(") or\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_DC).append("\"))\n")
                    .append("  |> drop(columns: [\"type\"]\n)")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )")
                    .append("\n");
        }else {
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(fiveMinAgo).append(", stop: ").append(now).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) =>\n")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_DEVICE).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_DC).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_INPUT_AC).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_BATTERY).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_OUTPUT_DC).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_OUTPUT_AC).append("\" or")
                    .append("    r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_GRID).append("\")\n")
                    .append("  |> drop(columns: [\"type\"])\n")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )");
        }

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getLastFiveMinutesCombined(List<? extends  Pair<SolarSystem,Boolean>> solarSystems,Long duration, Map<String,Integer> systemMappings) {

        Instant now = Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long sec = Duration.ofMillis(duration).getSeconds();
        sec = sec / NUM_TIME_STAMPS;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }

        now.plus((sec/2)-1,ChronoUnit.SECONDS);
        fiveMinAgo.minus((sec/2)-1,ChronoUnit.SECONDS);

        StringBuilder query = new StringBuilder(2048);

        for(int i=0;i<solarSystems.size();i++){
            var solarSystem = solarSystems.get(i).getKey();
            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query.append("d").append(i).append(" = ");
            }
            query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                    .append("  |> range(start: ").append(fiveMinAgo).append(", stop: ").append(now).append(")\n")
                    .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                    .append("  |> filter(fn: (r) =>\n")
                    .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\") and\n")
                    .append("    (r[\"_field\"] == \"InputWatt\"))\n")
                    .append("  |> aggregateWindow(every: ").append(sec).append("s, fn: mean )")
                    .append("  |> drop(columns: [\"type\"])\n")
                    .append("  |> map(fn: (r) => ({ _value:r._value, _time:r._time, _field:r._field+\"_").append(id).append("\" }))")
                    .append("\n\n");
        }

        if(solarSystems.size() > 1) {
            query.append("union(tables: [");

            var joiner = new StringJoiner(", ");

            for (int i = 0; i < solarSystems.size(); i++) {
                joiner.add("d" + i);
            }
            query.append(joiner.toString());

            query.append("])");
        }

        /*String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }else {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) => \n"+
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_AC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_BATTERY + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_DC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }*/

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getCombinedStatisticsDataAsJson(List<? extends Pair<SolarSystem,Boolean>> solarSystems,Date from ,Date to, Map<String,Integer> systemMappings) {

        if(solarSystems.isEmpty()){
            throw new RuntimeException("No solar systems given for combined statistics query");
        }

        StringBuilder query = new StringBuilder(4096);

        int i=0;
        for (Pair<SolarSystem, Boolean> pair : solarSystems) {
            var solarSystem = pair.getLeft();
            var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());

            var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
            var instantTo = ZonedDateTime.ofInstant(to.toInstant(), zId);

            Set<String> totalFilter = (solarSystem.getViewData() != null && solarSystem.getViewData().getTotalFilter() != null)
                ? solarSystem.getViewData().getTotalFilter()
                : Collections.emptySet();

            StringBuilder prodFilterBuilder = new StringBuilder();
            boolean firstCondition = true;

            if (!totalFilter.contains(InfluxFields.prodKWHField.getName())) {
                if (!firstCondition) prodFilterBuilder.append(" or\n");
                prodFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.prodKWHField).append("\"");
                firstCondition = false;
            }
            if (!totalFilter.contains(InfluxFields.calcProdKWHField.getName())) {
                if (!firstCondition) prodFilterBuilder.append(" or\n");
                prodFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcProdKWHField).append("\"");
                firstCondition = false;
            }
            if (!totalFilter.contains(InfluxFields.calcByDevicesProdKWHField.getName())) {
                if (!firstCondition) prodFilterBuilder.append(" or\n");
                prodFilterBuilder.append("    r[\"_field\"] == \"").append(InfluxFields.calcByDevicesProdKWHField).append("\"");
                firstCondition = false;
            }

            String prodFieldFilter = prodFilterBuilder.toString();

            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query.append("d").append(i).append(" = ");
            }
            //if (pair.getRight()) {
                query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                        .append("  |> range(start: ").append(zoneFormatter.format(instantFrom)).append(", stop:").append(zoneFormatter.format(instantTo)).append(")\n")
                        .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DAY_DATA).append("\")\n")
                        .append("  |> filter(fn: (r) => r.system == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                        .append("  |> filter(fn: (r) =>\n")
                        .append(prodFieldFilter).append(")\n")
                        .append("  |> drop(columns: [\"type\"]\n)")
                        .append("  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")")
                        .append("  |> map(fn: (r) => ({\n")
                        .append("     _time: r._time,\n")
                        .append("     _value: if exists r.").append(InfluxFields.prodKWHField).append(" then r.").append(InfluxFields.prodKWHField).append(" else if exists r.").append(InfluxFields.calcByDevicesProdKWHField).append(" then r.").append(InfluxFields.calcByDevicesProdKWHField).append(" else r.").append(InfluxFields.calcProdKWHField).append(",\n")
                        .append("     _field: \"").append(API_NAMING_PRODUCED).append("_").append(id).append("\" }))\n\n");
            i++;
        }

        if(solarSystems.size() > 1) {
            query.append("union(tables: [");

            var joiner = new StringJoiner(", ");

            for (int j = 0; j < solarSystems.size(); j++) {
                joiner.add("d" + j);
            }
            query.append(joiner.toString());

            query.append("])");
        }

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getLastCombinedStatisticsDataAsJson(List<? extends Pair<SolarSystem,Boolean>> solarSystems, Map<String,Integer> systemMappings) {

        Instant now = Instant.now();
        Instant twoDayAgo = now.minus(2, ChronoUnit.DAYS);

        return getCombinedStatisticsDataAsJson(solarSystems,new Date(twoDayAgo.toEpochMilli()),new Date(now.toEpochMilli()),systemMappings);
    }

    public void updatePrice(SolarSystem solarSystem,ZonedDateTime startDate){

        var now = ZonedDateTime.now();

        if(startDate != null){
            now = startDate;
        }

        var price = solarSystem.getSystemInformations() != null && solarSystem.getSystemInformations().getElectricityPrice() != null
                ? solarSystem.getSystemInformations().getElectricityPrice()
                : solarSystem.getElectricityPrice();

        var point = Point.measurement(SELDOM_CHANGING_STATS.getName())
                .time(now.toInstant().toEpochMilli(), WritePrecision.MS)
                .addField(InfluxFields.energyPriceMeasurement.getName(), price)
                .addTag("system", solarSystem.getInfluxTagName());

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);
    }

    public void updatePriceFeedIn(SolarSystem solarSystem,ZonedDateTime startDate){

        var now = ZonedDateTime.now();

        if(startDate != null){
            now = startDate;
        }

        var priceFeedIn = solarSystem.getSystemInformations() != null && solarSystem.getSystemInformations().getElectricityPriceFeedIn() != null
                ? solarSystem.getSystemInformations().getElectricityPriceFeedIn()
                : solarSystem.getElectricityPriceFeedIn();

        var point = Point.measurement(SELDOM_CHANGING_STATS.getName())
                .time(now.toInstant().toEpochMilli(), WritePrecision.MS)
                .addField(InfluxFields.energyPriceFeedInMeasurement.getName(), priceFeedIn)
                .addTag("system", solarSystem.getInfluxTagName());

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);
    }

    public List<FluxTable> getDevicePointsInTimeRange(SolarSystem solarSystem, Instant end, Duration duration){

        Instant instantTo = end.plus(10, ChronoUnit.SECONDS);
        Instant instantFrom = end.minus(duration);

        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                .append("  |> range(start: ").append(instantFrom).append(", stop: ").append(instantTo).append(")\n")
                .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\")\n")
                .append("  |> filter(fn: (r) =>\n")
                .append("    (r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA_DEVICE).append("\"))\n")
                .append("  |> last()\n")
                .append("\n\n");

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public long getSolarDataPointsForDay(SolarSystem solarSystem, LocalDate day){

        ZonedDateTime startOfDay = day.atStartOfDay(ZoneId.of("UTC"));
        Instant startInstant = startOfDay.toInstant();

        ZonedDateTime endOfDay = day.plusDays(1).atStartOfDay(ZoneId.of("UTC")).minusNanos(1);
        Instant endInstant = endOfDay.toInstant();

        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(solarSystem.getOwnedBy().getInfluxBucketName()).append("\")\n")
                .append("  |> range(start: ").append(startInstant).append(", stop: ").append(endInstant).append(")\n")
                .append("  |> filter(fn: (r) => ")
                .append("     r[\"_measurement\"] == \"").append(InfluxMeasurement.SOLAR_DATA).append("\" and\n")
                .append("     r[\"system\"] == \"").append(solarSystem.getInfluxTagName()).append("\" and\n")
                .append("     r._field == \"Duration\")\n")
                .append("  |> count()\n")
                .append("  |> keep(columns: [\"_value\"])");

        var res = influxConnection.getClient().getQueryApi().query(query.toString());

        if(res.isEmpty()){
            return 0;
        }

        return ((Number)(res.get(0).getRecords().get(0).getValue())).longValue();
    }
}
