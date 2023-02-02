package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

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
    public List<FluxTable> getStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to,boolean onlyProduction) {

        var system = solarSystemRepository.findById(systemId);
        system.setId(systemId);
        system.setRelationOwnedBy(User.builder().id(ownerId).build());
        var zId = ZoneId.of(system.getTimezone() == null ? "UTC" : system.getTimezone());

        var instantFrom = ZonedDateTime.ofInstant(from.toInstant(), zId);
        var instantTo= ZonedDateTime.ofInstant(to.toInstant(), zId);

        String query;
        if (onlyProduction) {
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + systemId + "\"\n)" +
                    "  |> filter(fn: (r) =>\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.calcProdKWHDCField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCField + "\" or\n" +
                    "    r[\"_field\"] == \"" + InfluxTaskService.prodKWHDCFieldSum + "\")" +
                    "\n";
        }else {
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + zoneFormatter.format(instantFrom) + ", stop:" + zoneFormatter.format(instantTo) + ")\n" +
                    "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DAY_DATA + "\")\n" +
                    "  |> filter(fn: (r) => r.system == \"" + systemId + "\"\n)" +
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
            influxTaskService.runUpdateLastDays(system, today);
        }

        var yesterday = today.minusDays(1);
        if(instantTo.isAfter(yesterday)){
            influxTaskService.runUpdateLastDays(system, yesterday);
        }

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public String generatePublicQueryParameters(){
        return " and (\n" +
                "      r[\"_field\"] == \"InputWattDC\" or\n" +
                "      r[\"_field\"] == \"InputVoltageDC\" or\n" +
                "      r[\"_field\"] == \"InputAmpereDC\")";
    }

    public List<FluxTable> getAllDataAsJson(long ownerId, long systemId,Date from, Date to,boolean onlyProduction) {

        Instant instantFrom=from.toInstant();
        Instant instantToday=to.toInstant();
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
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }else {
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + instantFrom + ", stop: " + instantToday + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
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


    public List<FluxTable> getLastFiveMin(long ownerId, long systemId, long duration,boolean onlyProduction) {

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
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + fiveMinAgo + ", stop: " + now + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
                    "  |> filter(fn: (r) =>\n" +
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_DEVICE + "\""+generatePublicQueryParameters()+ ") or\n"+
                    "    (r[\"_measurement\"] == \"" + InfluxMeasurement.SOLAR_DATA_INPUT_DC + "\"))\n" +
                    "  |> aggregateWindow(every: " + sec + "s, fn: mean )" +
                    "\n";
        }else {
            query = "from(bucket: \"user-" + ownerId + "\")\n" +
                    "  |> range(start: " + fiveMinAgo + ", stop: " + now + ")\n" +
                    "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
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
