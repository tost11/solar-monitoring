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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement.SELDOM_CHANGING_STATS;

@Service
public class InfluxService {

    public static final String API_NAMING_PRODUCED = "Produced";
    public static final String API_NAMING_CONSUMED = "Consumed";
    public static final String API_NAMING_BATTERY = "Battery";
    public static final String API_NAMING_DIFFERENCE = "Difference";
    public static final String API_NAMING_GRID_FEEDIN = "GridFeedIn";
    public static final String API_NAMING_GRID_CONSUMPTION = "GridConsumption";

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

        String base =
            "base = from(bucket: \"" + bucket + "\")\n" +
            "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop: " + zoneFormatter.format(instantTo) + ")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
            "  |> filter(fn: (r) => r.system == \"" + systemTag + "\")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcConsKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesConsKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.consKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcProdKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesProdKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.prodKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcBatteryKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesBatteryKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.batteryKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.gridConsKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.gridFeedInKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcGridConsKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcGridFeedInKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesGridConsKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesGridFeedInKWHField + "\"\n" +
            "  )\n" +
            "  |> pivot(rowKey: [\"_time\", \"system\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n";

        String baseOnlyProduction =
            "base = from(bucket: \"" + bucket + "\")\n" +
            "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop: " + zoneFormatter.format(instantTo) + ")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
            "  |> filter(fn: (r) => r.system == \"" + systemTag + "\")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcProdKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesProdKWHField + "\" or\n" +
            "    r[\"_field\"] == \"" + InfluxFields.prodKWHField + "\"\n" +
            "  )\n" +
            "  |> pivot(rowKey: [\"_time\", \"system\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n";

        String produced =
                "produced = base\n" +
                        "  |> map(fn: (r) => ({\n" +
                        "    _time: r._time,\n" +
                        "    system: r.system,\n" +
                        "    _measurement: r._measurement,\n" +
                        "    id: r.id,\n" +
                        "    _field: \""+API_NAMING_PRODUCED+"\",\n" +
                        "    _value: if exists r." + InfluxFields.prodKWHField + " then r." + InfluxFields.prodKWHField +
                        " else if exists r." + InfluxFields.calcByDevicesProdKWHField + " then r." + InfluxFields.calcByDevicesProdKWHField +
                        " else r." + InfluxFields.calcProdKWHField + "\n" +
                        "  }))\n";

        String consumed =
                "consumed = base\n" +
                        "  |> map(fn: (r) => ({\n" +
                        "    _time: r._time,\n" +
                        "    system: r.system,\n" +
                        "    _measurement: r._measurement,\n" +
                        "    id: r.id,\n" +
                        "    _field: \""+API_NAMING_CONSUMED+"\",\n" +
                        "    _value: if exists r." + InfluxFields.consKWHField + " then r." + InfluxFields.consKWHField +
                        " else if exists r." + InfluxFields.calcByDevicesConsKWHField + " then r." + InfluxFields.calcByDevicesConsKWHField +
                        " else r." + InfluxFields.calcConsKWHField + "\n" +
                        "  }))\n";

        String battery =
                "battery = base\n" +
                        "  |> map(fn: (r) => ({\n" +
                        "    _time: r._time,\n" +
                        "    system: r.system,\n" +
                        "    _measurement: r._measurement,\n" +
                        "    id: r.id,\n" +
                        "    _field: \""+API_NAMING_BATTERY+"\",\n" +
                        "    _value: if exists r." + InfluxFields.batteryKWHField + " then r." + InfluxFields.batteryKWHField +
                        " else if exists r." + InfluxFields.calcByDevicesBatteryKWHField + " then r." + InfluxFields.calcByDevicesBatteryKWHField +
                        " else r." + InfluxFields.calcBatteryKWHField + "\n" +
                        "  }))\n";

        String gridFeedIn =
                "gridFeedIn = base\n" +
                        "  |> map(fn: (r) => ({\n" +
                        "    _time: r._time,\n" +
                        "    system: r.system,\n" +
                        "    _measurement: r._measurement,\n" +
                        "    id: r.id,\n" +
                        "    _field: \""+API_NAMING_GRID_FEEDIN+"\",\n" +
                        "    _value: if exists r." + InfluxFields.gridFeedInKWHField + " then r." + InfluxFields.gridFeedInKWHField +
                        " else if exists r." + InfluxFields.calcByDevicesGridFeedInKWHField + " then r." + InfluxFields.calcByDevicesGridFeedInKWHField +
                        " else r." + InfluxFields.calcGridFeedInKWHField + "\n" +
                        "  }))\n";

        String gridConsumption =
                "gridConsumption = base\n" +
                        "  |> map(fn: (r) => ({\n" +
                        "    _time: r._time,\n" +
                        "    system: r.system,\n" +
                        "    _measurement: r._measurement,\n" +
                        "    id: r.id,\n" +
                        "    _field: \""+API_NAMING_GRID_CONSUMPTION+"\",\n" +
                        "    _value: if exists r." + InfluxFields.gridConsKWHField + " then r." + InfluxFields.gridConsKWHField +
                        " else if exists r." + InfluxFields.calcByDevicesGridConsKWHField + " then r." + InfluxFields.calcByDevicesGridConsKWHField +
                        " else r." + InfluxFields.calcGridConsKWHField + "\n" +
                        "  }))\n";

        String query;

        if (onlyProduction) {
            query = baseOnlyProduction + "\n" + produced +
                    "  |> keep(columns: [\"_time\", \"system\", \"_field\", \"_value\",\"id\",\"_measurement\"])\n" +
                    "  |> sort(columns: [\"_time\"])\n\n" +
                    "produced";
        } else {
            query = base + "\n" + produced + "\n" + consumed + "\n" + battery + "\n" + gridFeedIn + "\n" + gridConsumption + "\n" +
                    "union(tables: [produced, consumed, battery, gridFeedIn, gridConsumption])\n" +
                    "  |> keep(columns: [\"_time\", \"system\", \"_field\", \"_value\",\"id\",\"_measurement\"])\n" +
                    "  |> sort(columns: [\"_time\", \"_field\"])\n";
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getlastTwoDaysStatistic(SolarSystem solarSystem,InfluxMeasurement measurement,boolean onlyProduction) {

        Instant now = Instant.now();
        Instant twoDayAgo = now.minus(2, ChronoUnit.DAYS);

        return getStatisticsDataAsJson(solarSystem,measurement,new Date(twoDayAgo.toEpochMilli()),new Date(now.toEpochMilli()),onlyProduction);
    }

    public String generatePublicQueryParameters(){
        return " and (\n" +
                "      r[\"_field\"] == \"InputWattDC\" or\n" +
                "      r[\"_field\"] == \"InputVoltageDC\" or\n" +
                "      r[\"_field\"] == \"InputAmpereDC\")";
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
        String query;
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
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\" or\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_GRID + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }
        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getLastDayDataAsJson(SolarSystem solarSystem) {

        Instant instantFrom = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant instantToday = Instant.now();
        String query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) => (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\"))\n"+
                    "  |> last()" +
                    "\n";
        return influxConnection.getClient().getQueryApi().query(query);
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

        String query = "";

        for(int i=0;i<solarSystems.size();i++){
            var solarSystem = solarSystems.get(i).getKey();
            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query += "d"+i+" = ";
            }
            query += "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\") and\n" +
                    "    (r[\"_field\"] == \"InputWatt\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "  |> map(fn: (r) => ({ _value:r._value, _time:r._time, _field:r._field+\"_"+id+"\" }))"+
                    "\n\n";
        }

        if(solarSystems.size() > 1) {
            query += "union(tables: [";

            var joiner = new StringJoiner(", ");

            for (int i = 0; i < solarSystems.size(); i++) {
                joiner.add("d" + i);
            }
            query += joiner.toString();

            query += "])";
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

        return influxConnection.getClient().getQueryApi().query(query);
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

        String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + fiveMinAgo + ", stop: " + now + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }else {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + fiveMinAgo + ", stop: " + now + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_AC + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_BATTERY + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_DC + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\" or" +
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_GRID + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )";
        }

        return influxConnection.getClient().getQueryApi().query(query);
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

        String query = "";

        for(int i=0;i<solarSystems.size();i++){
            var solarSystem = solarSystems.get(i).getKey();
            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query += "d"+i+" = ";
            }
            query += "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + fiveMinAgo + ", stop: " + now + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\") and\n" +
                    "    (r[\"_field\"] == \"InputWatt\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "  |> map(fn: (r) => ({ _value:r._value, _time:r._time, _field:r._field+\"_"+id+"\" }))"+
                    "\n\n";
        }

        if(solarSystems.size() > 1) {
            query += "union(tables: [";

            var joiner = new StringJoiner(", ");

            for (int i = 0; i < solarSystems.size(); i++) {
                joiner.add("d" + i);
            }
            query += joiner.toString();

            query += "])";
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

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getCombinedStatisticsDataAsJson(List<? extends Pair<SolarSystem,Boolean>> solarSystems,Date from ,Date to, Map<String,Integer> systemMappings) {

        if(solarSystems.isEmpty()){
            throw new RuntimeException("No solar systems given for combined statistics query");
        }

        String query = "";

        int i=0;
        for (Pair<SolarSystem, Boolean> pair : solarSystems) {
            var solarSystem = pair.getLeft();
            var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());

            var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
            var instantTo = ZonedDateTime.ofInstant(to.toInstant(), zId);

            int id = systemMappings.get(solarSystem.getId());
            if(solarSystems.size() > 1){
                query += "d"+i+" = ";
            }
            //if (pair.getRight()) {
                query += "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                        "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                        "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                        "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                        "  |> filter(fn: (r) =>\n" +
                        "    r[\"_field\"] == \"" + InfluxFields.calcByDevicesProdKWHField + "\" or\n" + // not shur if this is correct
                        "    r[\"_field\"] == \"" + InfluxFields.calcProdKWHField + "\" or\n" + // not shur if this is correct
                        "    r[\"_field\"] == \"" + InfluxFields.prodKWHField + "\")\n" +
                        "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")" +
                        "  |> map(fn: (r) => ({\n" +
                        "     _time: r._time,\n" +
                        "     _value: if exists r."+InfluxFields.prodKWHField+" then r."+InfluxFields.prodKWHField+" else if exists r."+InfluxFields.calcByDevicesProdKWHField+" then r."+InfluxFields.calcByDevicesProdKWHField+" else r."+InfluxFields.calcProdKWHField+",\n" +
                        "     _field: \""+API_NAMING_PRODUCED+"_"+id+"\" }))\n\n";
            i++;
        }

        if(solarSystems.size() > 1) {
            query += "union(tables: [";

            var joiner = new StringJoiner(", ");

            for (int j = 0; j < solarSystems.size(); j++) {
                joiner.add("d" + j);
            }
            query += joiner.toString();

            query += "])";
        }

        return influxConnection.getClient().getQueryApi().query(query);
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

        var point = Point.measurement(SELDOM_CHANGING_STATS.getName())
                .time(now.toInstant().toEpochMilli(), WritePrecision.MS)
                .addField(InfluxFields.energyPriceMeasurement.getName(),solarSystem.getElectricityPrice())
                .addTag("system", solarSystem.getInfluxTagName());

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);
    }

    public void updatePriceFeedIn(SolarSystem solarSystem,ZonedDateTime startDate){

        var now = ZonedDateTime.now();

        if(startDate != null){
            now = startDate;
        }

        var point = Point.measurement(SELDOM_CHANGING_STATS.getName())
                .time(now.toInstant().toEpochMilli(), WritePrecision.MS)
                .addField(InfluxFields.energyPriceFeedInMeasurement.getName(),solarSystem.getElectricityPriceFeedIn())
                .addTag("system", solarSystem.getInfluxTagName());

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);
    }

    public List<FluxTable> getDevicePointsInTimeRange(SolarSystem solarSystem, Instant end, Duration duration){

        Instant instantTo = end.plus(10, ChronoUnit.SECONDS);
        Instant instantFrom = end.minus(duration);

        String query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                "  |> range(start: " + instantFrom + ", stop: " + instantTo + ")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                "  |> filter(fn: (r) =>\n" +
                "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\"))\n"+
                "  |> last()\n" +
                "\n\n";

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public long getSolarDataPointsForDay(SolarSystem solarSystem, LocalDate day){

        ZonedDateTime startOfDay = day.atStartOfDay(ZoneId.of("UTC"));
        Instant startInstant = startOfDay.toInstant();

        ZonedDateTime endOfDay = day.plusDays(1).atStartOfDay(ZoneId.of("UTC")).minusNanos(1);
        Instant endInstant = endOfDay.toInstant();

        String query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                "  |> range(start: " + startInstant + ", stop: " + endInstant + ")\n" +
                "  |> filter(fn: (r) => " +
                "     r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\" and\n" +
                "     r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\" and\n" +
                "     r._field == \"Duration\")\n" +
                "  |> count()\n" +
                "  |> keep(columns: [\"_value\"])";

        var res = influxConnection.getClient().getQueryApi().query(query);

        if(res.isEmpty()){
            return 0;
        }

        return ((Number)(res.get(0).getRecords().get(0).getValue())).longValue();
    }
}
