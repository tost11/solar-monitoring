package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;

import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InfluxService {
    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private InfluxTaskService influxTaskService;

    private DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    static private final int NUM_TIME_STAMPS = 60;
    public List<FluxTable> getStatisticsDataAsJson(SolarSystem solarSystem,Date from ,Date to,boolean onlyProduction) {

        var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());

        var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
        var instantTo= ZonedDateTime.ofInstant(to.toInstant(), zId);

        String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.calcProdKWHDCField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCFieldSum + "\")" +
                    "\n";
        }else {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.calcConsKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.calcProdKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.calcBatteryKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.consKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.batteryKWHField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHFieldSum + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.consKWHFieldSum + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.batteryKWHFieldSum + "\"\n" +
                    ")\n";
        }

        var today = ZonedDateTime.now(zId);
        today = today.withHour(0).withMinute(0).withSecond(0).withNano(0);

        if(instantTo.isAfter(today)){
            influxTaskService.runUpdateLastDays(solarSystem, today);
        }

        var yesterday = today.minusDays(1);
        if(instantTo.isAfter(yesterday)){
            influxTaskService.runUpdateLastDays(solarSystem, yesterday);
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getlastTwoDaysStatistic(SolarSystem solarSystem,boolean onlyProduction) {

        Instant now = Instant.now();
        Instant twoDayAgo = now.minus(2, ChronoUnit.DAYS);

        String query;
        if (onlyProduction) {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                "  |> range(start: " + zoneFormatter.format(twoDayAgo) + ", stop:" + zoneFormatter.format(now) + ")\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                "  |> filter(fn: (r) =>\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.calcProdKWHDCField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCFieldSum + "\")" +
                "\n";
        }else {
            query = "from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                "  |> range(start: " + twoDayAgo + ", stop:" + now + ")\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                "  |> filter(fn: (r) => r.system == \"" + solarSystem.getInfluxTagName() + "\"\n)" +
                "  |> filter(fn: (r) =>\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.calcConsKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.calcProdKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.calcBatteryKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.consKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.batteryKWHField + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHFieldSum + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.consKWHFieldSum + "\" or\n" +
                "    r[\"_field\"] == \"" + InfluxTaskService.batteryKWHFieldSum + "\"\n" +
                ")\n";
        }

        var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());

        var today = ZonedDateTime.now(zId);
        today = today.withHour(0).withMinute(0).withSecond(0).withNano(0);

        influxTaskService.runUpdateLastDays(solarSystem, today);
        var yesterday = today.minusDays(1);

        influxTaskService.runUpdateLastDays(solarSystem, yesterday);

        return influxConnection.getClient().getQueryApi().query(query);
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
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }


    public List<FluxTable> getProductionCombined(List<Pair<SolarSystem,Boolean>> solarSystems, Date from, Date to) {

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
            query += "d"+i+" = from(bucket: \"" + solarSystem.getOwnedBy().getInfluxBucketName() + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + solarSystem.getInfluxTagName() + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\") and\n" +
                    "    (r[\"_field\"] == \"InputWatt\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "  |> map(fn: (r) => ({ _value:r._value, _time:r._time, _field:r._field+\"_"+i+"\" }))"+
                    "\n\n";
        }

        query+="union(tables: [";

        var joiner = new StringJoiner(", ");

        for(int i=0;i<solarSystems.size();i++) {
            joiner.add("d" + i);
        }
        query+=joiner.toString();

        query+="])";

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

        System.out.println(query);

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
                    "    r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_OUTPUT_AC + "\")\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )";
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }
}
