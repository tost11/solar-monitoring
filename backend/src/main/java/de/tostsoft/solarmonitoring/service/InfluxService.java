package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.internal.shaded.io.netty.util.internal.StringUtil;
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

    static private final int NUM_TIME_STAMPS = 60;

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
        sec = sec / NUM_TIME_STAMPS;
        if(sec < 10){
            sec = 10;
        }
        if(sec >  60 * 5){
            sec = 60 * 5;
        }

        now.plus((sec/2)-1,ChronoUnit.SECONDS);
        fiveMinAgo.minus((sec/2)-1,ChronoUnit.SECONDS);

        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+fiveMinAgo+", stop: "+now+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+measurement+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to) {

        var system = solarSystemRepository.findById(systemId);
        system.setId(systemId);
        system.setRelationOwnedBy(User.builder().id(ownerId).build());
        var zId = ZoneId.of(system.getTimezone() == null ? "UTC" : system.getTimezone());

        var instantFrom= from.toInstant().atZone(zId).toInstant();
        var instantTo=to.toInstant().atZone(zId).toInstant();

        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \"day-values\")\n" +
            "  |> filter(fn: (r) => r.system == \""+systemId+"\")" +
            "  |> filter(fn: (r) => "+
                "r[\"_field\"] == \""+InfluxTaskService.calcConsKWHField+"\" or "+
                "r[\"_field\"] == \""+InfluxTaskService.calcProdKWHField+"\" or "+
                "r[\"_field\"] == \""+InfluxTaskService.prodKWHField+"\" or "+
                "r[\"_field\"] == \""+InfluxTaskService.consKWHField+"\"" +
            ")\n";

        var today = LocalDateTime.now().toLocalDate().atStartOfDay(zId);

        if(instantTo.isAfter(today.toInstant())){
            influxTaskService.runUpdateLastDays(system, today.toLocalDateTime());
        }

        var yesterday = today.minus(1,ChronoUnit.DAYS);
        if(instantTo.isAfter(yesterday.toInstant())){
            influxTaskService.runUpdateLastDays(system, yesterday.toLocalDateTime());
        }

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

        String query ="from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+fiveMinAgo+", stop: "+now+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> filter(fn: (r) => r[\"_measurement\"] == \""+InfluxMeasurement.GRID+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }
}
