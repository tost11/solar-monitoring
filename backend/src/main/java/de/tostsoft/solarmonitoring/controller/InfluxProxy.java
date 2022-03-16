package de.tostsoft.solarmonitoring.controller;

import com.influxdb.query.FluxColumn;
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

@RestController
@RequestMapping("/api/influx")
public class InfluxProxy {
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @PostMapping
    public GraphDTO getGraphCSV(@RequestBody CsvDTO csvDTO){

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem system= solarSystemRepository.findById(1);
        user.getGrafanaFolderId();
        if(system==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }

        String query ="from(bucket: \"user-0\")\n" +
                "  |> range(start: "+csvDTO.getFrom()+", stop: "+csvDTO.getTo()+")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \""+csvDTO.getSystemId()+"\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \""+csvDTO.getField()+"\")" ;

       var r= influxConnection.getClient().getQueryApi().query(query);
        for(FluxTable f:r){
            for(FluxColumn c: f.getColumns()){

            }
        }

       return null;

    }

}
