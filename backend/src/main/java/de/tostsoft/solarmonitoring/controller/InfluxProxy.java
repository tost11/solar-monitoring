package de.tostsoft.solarmonitoring.controller;

import com.influxdb.query.FluxColumn;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.dtos.CsvDTO;
import de.tostsoft.solarmonitoring.dtos.GraphDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public GraphDTO getGraphCSV(@RequestParam long systemId, @RequestParam Long from){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem system= solarSystemRepository.findByIdAndRelationOwnsOrRelationManageWithRelations(systemId,user.getId());
        if(system==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }

        Instant instantFrom=new Date(from).toInstant();
        Instant instantToday=new Date().toInstant();

        String query ="from(bucket: \"user-"+system.getRelationOwnedBy().getId()+"\")\n" +
                "  |> range(start: "+instantFrom+", stop: "+instantToday+")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \""+systemId+"\")" +
                "  |> aggregateWindow(every: 10m, fn: mean )" ;


        GraphDTO graphDTO=new GraphDTO();
        var r= influxConnection.getClient().getQueryApi().query(query);
        Map<String,List<Double>> data = new HashMap<>();
        List<Date> time = new ArrayList<>();
        for(FluxTable f:r){
            String label=(String) Objects.requireNonNull(f.getRecords().get(0).getValueByKey("_field"));
            List<Double>values = new ArrayList<>();
            for(FluxRecord t:r.get(0).getRecords()){
                time.add(Date.from((Instant) Objects.requireNonNull(t.getValueByKey("_time"))));
            }
            for(FluxRecord fluxRecord: f.getRecords()){
                values.add((Double) fluxRecord.getValueByKey("_value"));
            }
            data.put(label,values);
        }
        graphDTO.setTime(time);
        graphDTO.setData(data);

        System.out.println(data);
       return graphDTO;

    }

}
