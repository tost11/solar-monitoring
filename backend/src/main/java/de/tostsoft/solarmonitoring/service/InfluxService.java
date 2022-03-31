package de.tostsoft.solarmonitoring.service;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import java.time.Duration;
import java.time.Instant;
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

    public List<FluxTable> getAllDataAsJson(long userId, long systemId,Date from) {

        Instant instantFrom=from.toInstant();
        Instant instantToday=new Date().toInstant();
        long sec = Duration.between(instantFrom,instantToday).getSeconds();
        sec =sec/24;
        String query ="from(bucket: \"user-"+userId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop: "+instantToday+")\n" +
            "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
            "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable>  getStatisticDataAsJson(long userId, long systemId,Date from ,Date to) {

        Instant instantFrom=from.toInstant();
        Instant instantTo=to.toInstant();

        //Nicht schön aber geht
        String query ="t1=from(bucket: \"user-"+userId+"\")\n" +
            "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
            "  |> filter(fn: (r) =>\n" +
            "    (r._field == \"ChargeWatt\" or r._field == \"Duration\") and\n" +
            "    r.system == \""+systemId+"\"\n" +
            "  )\n" +
            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
            "  |> map(fn: (r) => ({ r with _value: r.ChargeWatt * r.Duration / 3600.0}))\n" +
            "  |> aggregateWindow(every: 1d,fn: sum)\n" +
            "  \n" +
            "t2=from(bucket: \"user-"+userId+"\")\n" +
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
            "t3=from(bucket: \"user-"+userId+"\")\n" +
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
}
