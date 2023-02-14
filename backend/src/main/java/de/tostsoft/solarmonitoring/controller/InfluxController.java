package de.tostsoft.solarmonitoring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.repository.MyAwesomeSolarSystemSaveRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.ConfigService;
import de.tostsoft.solarmonitoring.service.InfluxService;
import java.time.Instant;
import java.util.*;

import de.tostsoft.solarmonitoring.service.InfluxTaskService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/influx")
public class InfluxController {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MyAwesomeSolarSystemSaveRepository myAwesomeSolarSystemSaveRepository;
    @Autowired
    private InfluxService influxService;

    private void validateTimeRange(Date fromDate,Date toDate){
        if(toDate.before(fromDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"toDate can not be bevor from Date");
        }

        long diffInMillies = Math.abs(toDate.getTime() - fromDate.getTime());
        if(diffInMillies > 86400000L){//one day
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No time range longer than 1Day allowed");
        }
    }

    private Pair<Long, PublicMode> getCheckOwnerOrPublic(long systemId){
        Pair<Long, PublicMode> pair;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.isAuthenticated()) {
            User user = (User) auth.getPrincipal();
            long ownerID = -1;
            try {
                ownerID = userRepository.findOwnerIDByUserIDOrManagerID(systemId, user.getId());
            } catch (Exception e) {
            }
            if(ownerID != -1) {
                return new ImmutablePair(ownerID,null);
            }
        }
        try{
            //TODO move this here elsewhere
            var q = "MATCH (s) <- [:owns] - (ou:User) WHERE (s.publicMode = \"ALL\" or s.publicMode = \"PRODUCTION\" ) AND ID(s) = "+systemId+" return ID(ou) as userId,s.publicMode as publicMode";
            var res = myAwesomeSolarSystemSaveRepository.getDriver().session().readTransaction(tx->tx.run(q).single());
            pair = new ImmutablePair<>(res.values().get(0).asLong(),PublicMode.valueOf(res.values().get(1).asString()));
        }catch (Exception ex){
            LOG.debug(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no access on this System");
        }
        return pair;
    }

    private JsonArray convertToStatisticResult(final List<FluxTable> fluxResult){
        var res = (JsonArray)convertToGenericResult(fluxResult,false);
        for (JsonElement re : res) {
            var obj = re.getAsJsonObject();
            Float prodKWH = null;
            Float consKWH = null;
            Float batteryKWH = null;
            if(obj.has(InfluxTaskService.calcConsKWHField)){
                consKWH = obj.get(InfluxTaskService.calcConsKWHField).getAsFloat();
                obj.remove(InfluxTaskService.calcConsKWHField);
            }
            if(obj.has(InfluxTaskService.calcProdKWHField)){
                prodKWH = obj.get(InfluxTaskService.calcProdKWHField).getAsFloat();
                obj.remove(InfluxTaskService.calcProdKWHField);
            }
            if(obj.has(InfluxTaskService.calcProdKWHDCField)){
                prodKWH = obj.get(InfluxTaskService.calcProdKWHDCField).getAsFloat();
                obj.remove(InfluxTaskService.calcProdKWHDCField);
            }
            if(obj.has(InfluxTaskService.calcBatteryKWHField)){
                batteryKWH = obj.get(InfluxTaskService.calcBatteryKWHField).getAsFloat();
                obj.remove(InfluxTaskService.calcBatteryKWHField);
            }
            if(obj.has(InfluxTaskService.consKWHField)){
                consKWH = obj.get(InfluxTaskService.consKWHField).getAsFloat();
                obj.remove(InfluxTaskService.consKWHField);
            }
            if(obj.has(InfluxTaskService.prodKWHField)){
                prodKWH = obj.get(InfluxTaskService.prodKWHField).getAsFloat();
                obj.remove(InfluxTaskService.prodKWHField);
            }
            if(obj.has(InfluxTaskService.batteryKWHField)){
                batteryKWH = obj.get(InfluxTaskService.batteryKWHField).getAsFloat();
                obj.remove(InfluxTaskService.batteryKWHField);
            }
            if(obj.has(InfluxTaskService.consKWHFieldSum)){
                consKWH = obj.get(InfluxTaskService.consKWHFieldSum).getAsFloat();
                obj.remove(InfluxTaskService.consKWHFieldSum);
            }
            if(obj.has(InfluxTaskService.prodKWHFieldSum)){
                prodKWH = obj.get(InfluxTaskService.prodKWHFieldSum).getAsFloat();
                obj.remove(InfluxTaskService.prodKWHFieldSum);
            }
            if(obj.has(InfluxTaskService.prodKWHDCFieldSum)){
                prodKWH = obj.get(InfluxTaskService.prodKWHDCFieldSum).getAsFloat();
                obj.remove(InfluxTaskService.prodKWHDCFieldSum);
            }
            if(obj.has(InfluxTaskService.batteryKWHFieldSum)){
                batteryKWH = obj.get(InfluxTaskService.batteryKWHFieldSum).getAsFloat();
                obj.remove(InfluxTaskService.batteryKWHFieldSum);
            }
            if(prodKWH != null){
                obj.addProperty("Produced",prodKWH);
            }
            if(consKWH != null){
                obj.addProperty("Consumed",consKWH);
            }
            if(batteryKWH != null){
                obj.addProperty("Battery",batteryKWH);
            }
            if(prodKWH != null && consKWH != null){
                obj.addProperty("Difference",(prodKWH - consKWH));
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

        var map = new HashMap<Long,JsonObject>();

        for (FluxTable r : fluxResult) {
            for (FluxRecord record : r.getRecords()) {
                Long timestamp = ((Instant) record.getValueByKey("_time")).toEpochMilli();
                var jsonObject = map.get(timestamp);
                if (jsonObject == null) {
                    jsonObject = new JsonObject();
                    jsonObject.addProperty("time", timestamp);
                    map.put(timestamp, jsonObject);
                    jsonArray.add(jsonObject);
                }
                Number number = (Number) record.getValueByKey("_value");
                jsonObject.addProperty((String) Objects.requireNonNull(record.getValueByKey("_field")), number);
            }
        }

        if(rootIsObject){
            return rootObject;
        }
        return jsonArray;
    }

    private class TmpDeviceDTO{
        public HashSet<Long> inputDCIds = new HashSet<Long>();
        public HashSet<Long> inputACIds = new HashSet<Long>();
        public HashSet<Long> outputDCIds = new HashSet<Long>();
        public HashSet<Long> outputACIds = new HashSet<Long>();
        public HashSet<Long> batteryIds = new HashSet<Long>();
    }

    private TmpDeviceDTO addCrateDevice(HashMap<Long,TmpDeviceDTO> devices,long id){
        var v = devices.get(id);
        if(v != null){
            return v;
        }
        v = new TmpDeviceDTO();
        devices.put(id,v);
        return v;
    }

    private JsonObject convertToResult(final List<FluxTable> fluxResult){
        JsonObject rootObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        rootObject.add("data",jsonArray);
        if(fluxResult.size()==0){
            return rootObject;
        }

        var devices = new HashMap<Long,TmpDeviceDTO>();

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

                var obj = f.getRecords().get(i);
                var measurement = obj.getMeasurement();

                if(InfluxMeasurement.SOLAR_DATA.getName().equals(measurement)){
                    jsonObject.addProperty("" + obj.getValueByKey("_field"),number);
                }else{

                    if(number == null){
                        continue;
                    }

                    long id = Long.parseLong(""+f.getRecords().get(i).getValueByKey("id"));
                    if(InfluxMeasurement.SOLAR_DATA_DEVICE.getName().equals(measurement)){
                        addCrateDevice(devices,id);
                        jsonObject.addProperty(""+f.getRecords().get(i).getValueByKey("_field")+"-d-"+id, number);
                    }else {
                        long deviceId = Long.parseLong("" + f.getRecords().get(i).getValueByKey("deviceId"));
                        var device = addCrateDevice(devices, deviceId);

                        if (InfluxMeasurement.SOLAR_DATA_INPUT_DC.getName().equals(measurement)) {
                            jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field") + "-i-"+deviceId+"-"+id, number);
                            device.inputDCIds.add(id);
                        } else if (InfluxMeasurement.SOLAR_DATA_INPUT_AC.getName().equals(measurement)) {
                            jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field") + "-j-"+deviceId+"-"+id, number);
                            device.inputACIds.add(id);
                        } else if (InfluxMeasurement.SOLAR_DATA_OUTPUT_DC.getName().equals(measurement)) {
                            jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field") + "-o-"+deviceId+"-"+id, number);
                            device.outputDCIds.add(id);
                        } else if (InfluxMeasurement.SOLAR_DATA_OUTPUT_AC.getName().equals(measurement)) {
                            jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field") + "-c-"+deviceId+"-"+id, number);
                            device.outputACIds.add(id);
                        } else if (InfluxMeasurement.SOLAR_DATA_BATTERY.getName().equals(measurement)) {
                            jsonObject.addProperty("" + f.getRecords().get(i).getValueByKey("_field") + "-b-"+deviceId+"-"+id, number);
                            device.batteryIds.add(id);
                        }
                    }
                }
            }
            jsonArray.add(jsonObject);
        }

        var jsonDeviceMap = new JsonObject();
        devices.forEach((k,v)->{
            JsonObject o = new JsonObject();

            var arrInAC = new JsonArray(v.inputDCIds.size());
            v.inputDCIds.forEach(id->arrInAC.add(""+id));
            o.add("inputDCIds",arrInAC);

            var arrInDC = new JsonArray(v.inputACIds.size());
            v.inputACIds.forEach(id->arrInDC.add(""+id));
            o.add("inputACIds",arrInDC);

            var arrOutDC = new JsonArray(v.outputDCIds.size());
            v.outputDCIds.forEach(id->arrOutDC.add(""+id));
            o.add("outputDCIds",arrOutDC);

            var arrOutAC = new JsonArray(v.outputACIds.size());
            v.outputACIds.forEach(id->arrOutAC.add(""+id));
            o.add("outputACIds",arrOutAC);

            var arrBat = new JsonArray(v.batteryIds.size());
            v.batteryIds.forEach(id->arrBat.add(""+id));
            o.add("batteryIds",arrBat);

            jsonDeviceMap.add(""+k,o);
        });

        rootObject.add("devices", jsonDeviceMap);

        return rootObject;
    }

    @GetMapping("/all")
    public String getAllData(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        var pairIdPublic = getCheckOwnerOrPublic(systemId);

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var fluxResult = influxService.getAllDataAsJson(pairIdPublic.getLeft(),systemId,fromDate, toDate,pairIdPublic.getRight() == PublicMode.PRODUCTION);
        return convertToResult(fluxResult).toString();
    }

    @GetMapping("/statistics/all")
    public String getProduceStats(@RequestParam long systemId, @RequestParam Long from,@RequestParam Long to){
        var pairIdPublic = getCheckOwnerOrPublic(systemId);
        //TODO validate time range
        var fluxResult = influxService.getStatisticsDataAsJson(pairIdPublic.getLeft(), systemId, new Date(from), new Date(to),pairIdPublic.getRight() == PublicMode.PRODUCTION);
        return convertToStatisticResult(fluxResult).toString();
    }

    @GetMapping("/statistics/latest")
    public String getProduceStatsLatest(@RequestParam long systemId){
        var pairIdPublic = getCheckOwnerOrPublic(systemId);
        //TODO validate time range
        var fluxResult = influxService.getlastTwoDaysStatistic(pairIdPublic.getLeft(), systemId,pairIdPublic.getRight() == PublicMode.PRODUCTION);
        return convertToStatisticResult(fluxResult).toString();
    }

    @GetMapping("/latest")
    public String getLast5Min(@RequestParam long systemId,@RequestParam long duration){
        var pairIdPublic = getCheckOwnerOrPublic(systemId);

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var fluxResult = influxService.getLastFiveMin(pairIdPublic.getLeft(),systemId,duration,pairIdPublic.getRight() == PublicMode.PRODUCTION);
        return convertToResult(fluxResult).toString();
    }
}
