package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InfluxService {
    @Autowired
    private InfluxConnection influxConnection;

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

    public List<FluxTable> getSelfmadeStatisticsDataAsJson(long ownerId, long systemId,Date from ,Date to) {

        Instant instantFrom=from.toInstant();
        Instant instantTo=to.toInstant();

        //Nicht schön aber geht
        String query ="t1=from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"ChargeWatt\" or r._field == \"Duration\") and\n" +
            "    r.system == \""+systemId+"\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: r.ChargeWatt * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n" +
            "  \n" +
            "t2=from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"TotalConsumption\" or r._field == \"Duration\") and\n" +
            "    r.system == \""+systemId+"\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: r.TotalConsumption * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n" +
            "\n" +
            "t4=join(tables: {t1: t1, t2: t2}, on: [\"_time\",\"_start\",\"_stop\"])\n" +
            "\n" +
            "t3=from(bucket: \"user-"+ownerId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"TotalConsumption\" or r._field == \"Duration\" or r._field == \"ChargeWatt\" ) and\n" +
            "    r.system == \""+systemId+"\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: (r.ChargeWatt - r.TotalConsumption) * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n" +
            "\n" +
            "join(tables: {t3: t3, t4: t4}, on:  [\"_time\",\"_start\",\"_stop\"])";

        return influxConnection.getClient().getQueryApi().query(query);
    }


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
    }


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
            "  |> filter(fn: (r) => r[\"id\"] == \""+0+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }


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
    }

}
