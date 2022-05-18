package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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

    public List<FluxTable> getAllDataAsJson(long ownerId, long systemId, InfluxMeasurement measurement,Date from, Date to) {

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
        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop: "+instantToday+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+measurement+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getLastFiveMin(long ownerId, long systemId,InfluxMeasurement measurement, long duration) {

        Instant now=Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long sec = Duration.ofMillis(duration).getSeconds();
        sec = sec / 60;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }
        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+fiveMinAgo+", stop: "+now+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+measurement+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to) {

        //TODO refactor with anything faster
        var system = solarSystemRepository.findById(systemId);
        var zId = ZoneId.of(system.getTimezone() == null ? "UTC" : system.getTimezone());

        var instantFrom= from.toInstant().atZone(zId).toInstant();
        var instantTo=to.toInstant().atZone(zId).toInstant();

        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \"day-values\")\n" +
            "  |> filter(fn: (r) => r.system == \""+systemId+"\")" +
            "  |> filter(fn: (r) => r[\"_field\"] == \"calcConsumedKWH\" or r[\"_field\"] == \"calcProducedKWH\")\n";

        return influxConnection.getClient().getQueryApi().query(query);
    }

    /*
    public List<FluxTable> getSimpleStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to) {

        Instant instantFrom=from.toInstant();
        Instant instantTo=to.toInstant();

        //Nicht schön aber geht
        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"ChargeWatt\" or r._field == \"Duration\") and\n" +
            "    r.system == \""+systemId+"\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: r.ChargeWatt * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n"+
            "\n";

        return influxConnection.getClient().getQueryApi().query(query);
    }*/


    public List<FluxTable> getGridAllDataAsJson(long ownerId, long systemId,Date from, Date to) {

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
        String query = "from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop: "+instantToday+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.GRID+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" +
            "\n";

        return influxConnection.getClient().getQueryApi().query(query);
    }


    public List<FluxTable> getGridLastFiveMin(long ownerId, long systemId, long duration) {

        Instant now=Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long sec = Duration.ofMillis(duration).getSeconds();
        sec = sec / 60;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }
        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+fiveMinAgo+", stop: "+now+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.GRID+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }


    /*
    public List<FluxTable> getGridStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to) {

        Instant instantFrom=from.toInstant();
        Instant instantTo=to.toInstant();

        //Nicht schön aber geht
        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"GridWatt\" or r._field == \"Duration\") and\n" +
            "    r.system == \""+systemId+"\" and\n" +
            "    r.id == \"0\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: r.GridWatt * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n"+
            "\n";

        return influxConnection.getClient().getQueryApi().query(query);
    }*/

}
