package de.tostsoft.solarmonitoring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.InfluxService;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/influx")
public class InfluxController {
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InfluxService influxService;

    @GetMapping("/getAllData")
    public String getAllData(@RequestParam long systemId, @RequestParam Long from){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long ownerID;
        try{
             ownerID= userRepository.findOwnerIDByUserIDOrManagerID(systemId,user.getId());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }

        var fluxResult = influxService.getAllDataAsJson(ownerID,systemId,new Date(from));
        JsonArray jsonArray =new JsonArray();
        for(int i=0; i<fluxResult.get(0).getRecords().size();i++){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", ((Instant) fluxResult.get(0).getRecords().get(i).getValueByKey("_time")).toEpochMilli());
            for(FluxTable f:fluxResult){

                jsonObject.addProperty((String) Objects.requireNonNull(f.getRecords().get(i).getValueByKey("_field")),(Number) f.getRecords().get(i).getValueByKey("_value"));
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray.toString();
    }

    @GetMapping("/Statistics")
    public String getProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long ownerID;
        try{
            ownerID= userRepository.findOwnerIDByUserIDOrManagerID(systemId,user.getId());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }
        JsonArray jsonArray =new JsonArray();
        var fluxResult = influxService.getStatisticDataAsJson(ownerID, systemId, new Date(from), new Date(to));
        for(int i=0;i<fluxResult.size();i++){
            for(FluxRecord record:fluxResult.get(i).getRecords()){
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
