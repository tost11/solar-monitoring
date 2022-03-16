package de.tostsoft.solarmonitoring.controller;

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
                "  |> range(start: -1h, stop: now())\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \"1\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"ChargeWatt\")" ;


        String re="#group,false,false,true,true,false,false,true,true,true,true\n#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,double,string,string,string,string\n#default,mean,,,,,,,,,\n";


       var r= influxConnection.getClient().getQueryApi().queryRaw(query);
       re=re+r;


       return new GraphDTO(re);

    }

}
