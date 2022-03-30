package de.tostsoft.solarmonitoring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/influx")
public class InfluxProxy {
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @GetMapping("/getAllData")
    public String getGraphCSV(@RequestParam long systemId, @RequestParam Long from){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem system= solarSystemRepository.findByIdAndRelationOwnsOrRelationManageWithRelations(systemId,user.getId());
        if(system==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }

        Instant instantFrom=new Date(from).toInstant();
        Instant instantToday=new Date().toInstant();
        long sec = Duration.between(instantFrom,instantToday).getSeconds();
        sec =sec/24;
        String query ="from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n" +
                "  |> range(start: "+instantFrom+", stop: "+instantToday+")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")\n" +
                "  |> aggregateWindow(every: "+sec+"s, fn: mean )" ;


        JsonArray jsonArray =new JsonArray();
        var r= influxConnection.getClient().getQueryApi().query(query);
        for(int i=0; i<r.get(0).getRecords().size();i++){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", ((Instant) r.get(0).getRecords().get(i).getValueByKey("_time")).toEpochMilli());
            for(FluxTable f:r){

                jsonObject.addProperty((String) Objects.requireNonNull(f.getRecords().get(i).getValueByKey("_field")),(Number) f.getRecords().get(i).getValueByKey("_value"));
            }
            jsonArray.add(jsonObject);
        }
       return jsonArray.toString();

    }
    @GetMapping("/Statistics")
    public String getProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem system= solarSystemRepository.findByIdAndRelationOwnsOrRelationManageWithRelations(systemId,user.getId());
        if(system==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }

        Instant instantFrom=new Date(from).toInstant();
        Instant instantTo=new Date(to).toInstant();

        //Nicht schön aber geht
        String query ="t1=from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n" +
                "  |> range(start: "+instantFrom+", stop:"+instantTo+")\n" +
                "  |> filter(fn: (r) =>\n" +
                "    (r._field == \"ChargeWatt\" or r._field == \"Duration\") and\n" +
                "    r.system == \""+systemId+"\"\n" +
                "  )\n" +
                "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\" )\n" +
                "  |> map(fn: (r) => ({ r with _value: r.ChargeWatt * r.Duration / 3600.0}))\n" +
                "  |> aggregateWindow(every: 1d,fn: sum)\n" +
                "  \n" +
                "t2=from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n" +
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
                "t3=from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n" +
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


        JsonArray jsonArray =new JsonArray();
        var r= influxConnection.getClient().getQueryApi().query(query);


        for(int i=0;i<r.size();i++){
            for(FluxRecord record:r.get(i).getRecords()){
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("time", ((Instant) record.getValueByKey("_time")).toEpochMilli());
                //Difference
                jsonObject.addProperty("Difference",(Number) record.getValueByKey("_value"));
                //Produce
                jsonObject.addProperty("Produce",(Number) record.getValueByKey("_value_t1"));
                //Consumption
                jsonObject.addProperty("Consumption",(Number) record.getValueByKey("_value_t2"));
                jsonArray.add(jsonObject);
            }

        }
        return jsonArray.toString();

    }


}
