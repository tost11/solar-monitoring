package de.tostsoft.solarmonitoring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.InfluxService;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

    static public final List<String> SELFMADE_SYSTEM_TYPES = Arrays.asList(
        SolarSystemType.SELFMADE.toString(),
        SolarSystemType.SELFMADE_CONSUMPTION.toString(),
        SolarSystemType.SELFMADE_INVERTER.toString(),
        SolarSystemType.SELFMADE_DEVICE.toString());

    static public final List<String> SIMPLE_SYSTEM_TYPES = Arrays.asList(
        SolarSystemType.SIMPLE.toString(),
        SolarSystemType.VERY_SIMPLE.toString());

    static public final List<String> GRID_SYSTEM_TYPES = Arrays.asList(
        SolarSystemType.GRID.toString());

    private void validateTimeRange(Date fromDate,Date toDate){
        if(toDate.before(fromDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"toDate can not be bevor from Date");
        }

        long diffInMillies = Math.abs(toDate.getTime() - fromDate.getTime());
        if(diffInMillies > 86400000L){//one day
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No time range longer than 1Day allowed");
        }
    }

    private long getCheckOwner(long systemId,final List<String> types){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long ownerID;
        try{
            ownerID = userRepository.findOwnerIDByUserIDOrManagerIDAdSystemTypeIn(systemId,user.getId(),types);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have no access on this System");
        }
        return ownerID;
    }



    private JsonElement convertToGenericResult(final List<FluxTable> fluxResult){
        return convertToGenericResult(fluxResult,true);
    }

    private JsonArray convertToStatisticResult(final List<FluxTable> fluxResult){
        var res = (JsonArray)convertToGenericResult(fluxResult,false);
        for (JsonElement re : res) {
            var obj = re.getAsJsonObject();
            Float prodKWH = null;
            Float consKWH = null;
            if(obj.has("calcProducedKWH")){
                prodKWH = obj.get("calcProducedKWH").getAsFloat();
                obj.remove("calcProducedKWH");
            }
            if(obj.has("calcConsumedKWH")){
                consKWH = obj.get("calcConsumedKWH").getAsFloat();
                obj.remove("calcConsumedKWH");
            }
            if(prodKWH != null){
                obj.addProperty("Produced",prodKWH*1000);
            }
            if(consKWH != null){
                obj.addProperty("Consumed",consKWH*1000);
            }
            if(prodKWH != null && consKWH != null){
                obj.addProperty("Difference",(prodKWH - consKWH)*1000);
            }
        }
        return res;
    }

    private JsonElement convertToGenericResult(final List<FluxTable> fluxResult,boolean rootIsObject){
        JsonObject rootObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        rootObject.add("data",jsonArray);
        if(fluxResult.size()==0){
            if(rootIsObject){
                return rootObject;
            }
            return jsonArray;
        }
        for(int i=0; i<fluxResult.get(0).getRecords().size();i++){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", ((Instant) fluxResult.get(0).getRecords().get(i).getValueByKey("_time")).toEpochMilli());
            for(FluxTable f:fluxResult){
                Number number = (Number) f.getRecords().get(i).getValueByKey("_value");
                if (number instanceof Float){
                    number = Math.round((Float) number*100.f)/100.f;
                }
                if (number instanceof Double){
                    number = Math.round((Double) number*100.)/100.;
                }
                jsonObject.addProperty((String) Objects.requireNonNull(f.getRecords().get(i).getValueByKey("_field")),number);
            }
            jsonArray.add(jsonObject);
        }
        if(rootIsObject){
            return rootObject;
        }
        return jsonArray;
    }

    @GetMapping("/selfmade/all")
    public String getAllData(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,SELFMADE_SYSTEM_TYPES);

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var fluxResult = influxService.getAllDataAsJson(ownerID,systemId, InfluxMeasurement.SELFMADE,fromDate, toDate);
        return convertToGenericResult(fluxResult).toString();
    }

    @GetMapping("/selfmade/statistics")
    public String getProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,SELFMADE_SYSTEM_TYPES);
        //TODO validate time range
        JsonArray jsonArray = new JsonArray();
        var fluxResult = influxService.getStatisticsDataAsJson(ownerID, systemId, new Date(from), new Date(to));
        var res = convertToStatisticResult(fluxResult);
        return res.toString();
    }

    @GetMapping("/selfmade/latest")
    public String getLast5Min(@RequestParam long systemId,@RequestParam long duration){
        long ownerID = getCheckOwner(systemId,SELFMADE_SYSTEM_TYPES);

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var fluxResult = influxService.getLastFiveMin(ownerID,systemId, InfluxMeasurement.SELFMADE,duration);
        return convertToGenericResult(fluxResult).toString();
    }


    // --------------------------------------------------- simple --------------------------------------------------------

    @GetMapping("/simple/all")
    public String getSimpleAllData(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,SIMPLE_SYSTEM_TYPES);

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var fluxResult = influxService.getAllDataAsJson(ownerID,systemId, InfluxMeasurement.SIMPLE,fromDate, toDate);
        return convertToGenericResult(fluxResult).toString();
    }

    @GetMapping("/simple/statistics")
    public String getSimpleProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,SIMPLE_SYSTEM_TYPES);
        //TODO validate time range
        var fluxResult = influxService.getStatisticsDataAsJson(ownerID, systemId, new Date(from), new Date(to));
        return convertToStatisticResult(fluxResult).toString();
    }

    @GetMapping("/simple/latest")
    public String getSimpleLast5Min(@RequestParam long systemId,@RequestParam long duration){
        long ownerID = getCheckOwner(systemId,SIMPLE_SYSTEM_TYPES);

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var fluxResult = influxService.getLastFiveMin(ownerID,systemId, InfluxMeasurement.SIMPLE,duration);
        return convertToGenericResult(fluxResult).toString();
    }

    // --------------------------------------------------- simple --------------------------------------------------------

    private JsonObject convertToGridResult(final List<FluxTable> fluxResult){
        JsonObject rootObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        rootObject.add("data",jsonArray);
        if(fluxResult.size()==0){
            return rootObject;
        }

        var deviceIds = new HashSet<Long>();

        for(int i=0; i<fluxResult.get(0).getRecords().size();i++){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", ((Instant) fluxResult.get(0).getRecords().get(i).getValueByKey("_time")).toEpochMilli());
            for(FluxTable f:fluxResult){
                Number number = (Number) f.getRecords().get(i).getValueByKey("_value");
                if (number instanceof Float){
                    number = Math.round((Float) number*100.f)/100.f;
                }
                if (number instanceof Double){
                    number = Math.round((Double) number*100.)/100.;
                }
                Long id = Long.parseLong(""+f.getRecords().get(i).getValueByKey("id"));
                if(id == 0){
                    jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field"),number);
                }else{
                    jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field")+"_"+id,number);
                    deviceIds.add(id);
                }
            }
            jsonArray.add(jsonObject);
        }

        var jsonDeviceArray = new JsonArray(deviceIds.size());
        deviceIds.forEach(jsonDeviceArray::add);

        rootObject.add("deviceIds",jsonDeviceArray);

        return rootObject;
    }


    @GetMapping("/grid/all")
    public String getGridAllData(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,GRID_SYSTEM_TYPES);

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var fluxResult = influxService.getGridAllDataAsJson(ownerID,systemId,fromDate, toDate);
        return convertToGridResult(fluxResult).toString();
    }


    @GetMapping("/grid/statistics")
    public String getGridProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        long ownerID = getCheckOwner(systemId,GRID_SYSTEM_TYPES);
        //TODO validate time range
        var fluxResult = influxService.getStatisticsDataAsJson(ownerID, systemId, new Date(from), new Date(to));
        return convertToStatisticResult(fluxResult).toString();
    }

    @GetMapping("/grid/latest")
    public String getGridLast5Min(@RequestParam long systemId,@RequestParam long duration){
        long ownerID = getCheckOwner(systemId,GRID_SYSTEM_TYPES);

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var fluxResult = influxService.getGridLastFiveMin(ownerID,systemId,duration);
        return convertToGridResult(fluxResult).toString();
    }



}
