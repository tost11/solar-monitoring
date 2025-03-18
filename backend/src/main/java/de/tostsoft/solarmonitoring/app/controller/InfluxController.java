package de.tostsoft.solarmonitoring.app.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.app.monitoring.ApiMeterRegistry;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxFields;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.app.service.SolarSystemService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/api/influx")
public class InfluxController {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxController.class);

    @Autowired
    private ApiMeterRegistry apiMeterRegistry;

    @Autowired
    private InfluxService influxService;

    @Autowired
    private SolarSystemService solarSystemService;

    private void validateTimeRange(Date fromDate,Date toDate){
        if(toDate.before(fromDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"toDate can not be bevor from Date");
        }

        long diffInMillies = Math.abs(toDate.getTime() - fromDate.getTime());
        if(diffInMillies > 86400000L){//one day
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No time range longer than 1Day allowed");
        }
    }



    private JsonArray convertToStatisticResult(final List<FluxTable> fluxResult){
        var res = (JsonArray)convertToGenericResult(fluxResult,false);
        for (JsonElement re : res) {
            var obj = re.getAsJsonObject();
            Float prodKWH = null;
            Float consKWH = null;
            Float batteryKWH = null;
            if(obj.has(InfluxFields.calcConsKWHField.getName())){
                consKWH = obj.get(InfluxFields.calcConsKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.calcConsKWHField.getName());
            }
            if(obj.has(InfluxFields.calcProdKWHField.getName())){
                prodKWH = obj.get(InfluxFields.calcProdKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.calcProdKWHField.getName());
            }
            /* Maby this is used later when implemented
            if(obj.has(InfluxFields.calcProdKWHDCField.getName())){
                prodKWH = obj.get(InfluxFields.calcProdKWHDCField.getName()).getAsFloat();
                obj.remove(InfluxFields.calcProdKWHDCField.getName());
            }*/
            if(obj.has(InfluxFields.calcBatteryKWHField.getName())){
                batteryKWH = obj.get(InfluxFields.calcBatteryKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.calcBatteryKWHField.getName());
            }
            if(obj.has(InfluxFields.consKWHField.getName())){
                consKWH = obj.get(InfluxFields.consKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.consKWHField.getName());
            }
            if(obj.has(InfluxFields.prodKWHField.getName())){
                prodKWH = obj.get(InfluxFields.prodKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.prodKWHField.getName());
            }
            /* Maby this is used later when implemented
            if(obj.has(InfluxFields.prodKWHDCField.getName())){
                prodKWH = obj.get(InfluxFields.prodKWHDCField.getName()).getAsFloat();
                obj.remove(InfluxFields.prodKWHDCField.getName());
            }*/
            if(obj.has(InfluxFields.batteryKWHField.getName())){
                batteryKWH = obj.get(InfluxFields.batteryKWHField.getName()).getAsFloat();
                obj.remove(InfluxFields.batteryKWHField.getName());
            }
            /*if(obj.has(InfluxTaskService.consKWHFieldSum)){
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
            }*/
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


    private JsonArray convertToCombinedStatisticResult(final List<FluxTable> fluxResult){
        var mappedObjects = new HashMap<Long,JsonObject>();
        var res = (JsonArray)convertToGenericResult(fluxResult,false);
        List<JsonElement> indexesToRemove = new ArrayList<>();
        for (JsonElement re : res) {
            var obj = re.getAsJsonObject();

            Set<String> subs = new HashSet<>();

            for (Map.Entry<String, JsonElement> stringJsonElementEntry : obj.entrySet()) {
                String s = stringJsonElementEntry.getKey();
                int index = s.lastIndexOf("_");
                if (index == -1) {
                    continue;
                }
                String sub = s.substring(index);
                subs.add(sub);
            }


            long days = TimeUnit.MILLISECONDS.toDays(obj.get("time").getAsLong()) + 1; //TODO find out why this +1 is needed
            long newMillis = TimeUnit.DAYS.toMillis(days);

            var letObjToAdd = obj;

            if (mappedObjects.containsKey(newMillis)) {
                letObjToAdd = mappedObjects.get(newMillis);
                indexesToRemove.add(obj);
            } else {
                mappedObjects.put(newMillis, obj);
                letObjToAdd.addProperty("time", newMillis);
            }

            for (String sub : subs) {

                Float prodKWH = null;
                Float consKWH = null;
                Float batteryKWH = null;
                if (obj.has(InfluxFields.calcConsKWHField + sub)) {
                    consKWH = obj.get(InfluxFields.calcConsKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.calcConsKWHField + sub);
                }
                if (obj.has(InfluxFields.calcProdKWHField + sub)) {
                    prodKWH = obj.get(InfluxFields.calcProdKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.calcProdKWHField + sub);
                }
                if (obj.has(InfluxFields.calcProdKWHDCField + sub)) {
                    prodKWH = obj.get(InfluxFields.calcProdKWHDCField + sub).getAsFloat();
                    obj.remove(InfluxFields.calcProdKWHDCField + sub);
                }
                if (obj.has(InfluxFields.calcBatteryKWHField + sub)) {
                    batteryKWH = obj.get(InfluxFields.calcBatteryKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.calcBatteryKWHField + sub);
                }
                if (obj.has(InfluxFields.consKWHField + sub)) {
                    consKWH = obj.get(InfluxFields.consKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.consKWHField + sub);
                }
                if (obj.has(InfluxFields.prodKWHField + sub)) {
                    prodKWH = obj.get(InfluxFields.prodKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.prodKWHField + sub);
                }
                if (obj.has(InfluxFields.batteryKWHField + sub)) {
                    batteryKWH = obj.get(InfluxFields.batteryKWHField + sub).getAsFloat();
                    obj.remove(InfluxFields.batteryKWHField + sub);
                }
                /*if (obj.has(InfluxTaskService.consKWHFieldSum + sub)) {
                    consKWH = obj.get(InfluxTaskService.consKWHFieldSum + sub).getAsFloat();
                    obj.remove(InfluxTaskService.consKWHFieldSum + sub);
                }
                if (obj.has(InfluxTaskService.prodKWHFieldSum + sub)) {
                    prodKWH = obj.get(InfluxTaskService.prodKWHFieldSum + sub).getAsFloat();
                    obj.remove(InfluxTaskService.prodKWHFieldSum + sub);
                }
                if (obj.has(InfluxTaskService.prodKWHDCFieldSum + sub)) {
                    prodKWH = obj.get(InfluxTaskService.prodKWHDCFieldSum + sub).getAsFloat();
                    obj.remove(InfluxTaskService.prodKWHDCFieldSum + sub);
                }
                if (obj.has(InfluxTaskService.batteryKWHFieldSum + sub)) {
                    batteryKWH = obj.get(InfluxTaskService.batteryKWHFieldSum + sub).getAsFloat();
                    obj.remove(InfluxTaskService.batteryKWHFieldSum + sub);
                }*/
                if (prodKWH != null) {
                    letObjToAdd.addProperty("Produced" + sub, prodKWH);
                }
                if (consKWH != null) {
                    letObjToAdd.addProperty("Consumed" + sub, consKWH);
                }
                if (batteryKWH != null) {
                    letObjToAdd.addProperty("Battery" + sub, batteryKWH);
                }
                if (prodKWH != null && consKWH != null) {
                    letObjToAdd.addProperty("Difference" + sub, (prodKWH - consKWH));
                }
            }
        }
        for (JsonElement jsonElement : indexesToRemove) {
            res.remove(jsonElement);
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

    private JsonObject convertToResult(final List<FluxTable> fluxResult,boolean withIdMapping){
        JsonObject rootObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        rootObject.add("data",jsonArray);
        if(fluxResult.isEmpty()){
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

                if(!withIdMapping || InfluxMeasurement.SOLAR_DATA.getName().equals(measurement)){
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

        if(withIdMapping) {
            var jsonDeviceMap = new JsonObject();
            devices.forEach((k, v) -> {
                JsonObject o = new JsonObject();

                var arrInAC = new JsonArray(v.inputDCIds.size());
                v.inputDCIds.forEach(id -> arrInAC.add("" + id));
                o.add("inputDCIds", arrInAC);

                var arrInDC = new JsonArray(v.inputACIds.size());
                v.inputACIds.forEach(id -> arrInDC.add("" + id));
                o.add("inputACIds", arrInDC);

                var arrOutDC = new JsonArray(v.outputDCIds.size());
                v.outputDCIds.forEach(id -> arrOutDC.add("" + id));
                o.add("outputDCIds", arrOutDC);

                var arrOutAC = new JsonArray(v.outputACIds.size());
                v.outputACIds.forEach(id -> arrOutAC.add("" + id));
                o.add("outputACIds", arrOutAC);

                var arrBat = new JsonArray(v.batteryIds.size());
                v.batteryIds.forEach(id -> arrBat.add("" + id));
                o.add("batteryIds", arrBat);

                jsonDeviceMap.add("" + k, o);
            });

            rootObject.add("devices", jsonDeviceMap);
        }

        return rootObject;
    }

    private Map<String,Float> extractSingleResult(List<FluxTable> fluxTables){
        var ret = new HashMap<String,Float>();

        for (FluxTable fluxTable : fluxTables) {
            if(fluxTable.getRecords().isEmpty()){
                continue;
            }
            var rec = fluxTable.getRecords().get(0);
            var mes = rec.getValueByKey("_field");
            var val = rec.getValueByKey("_value");
            if(mes == null || val == null){
                continue;
            }
            ret.put(mes.toString(),((Number)val).floatValue());
        }
        return ret;
    }

    private void addIfPresent(JsonObject obj,Map<String,Float> map,String name,String as){
        Float value = map.get(name);
        if(value == null){
            return;
        }
        if(value <= 0){
            return;
        }
        obj.addProperty(as,value);
    }

    private Float getIfOverZero(Float value){
        if(value == null){
            return null;
        }
        if(value <= 0){
            return null;
        }
        return value;
    }

    private void totalValuesToJsonObject(Pair<SolarSystem, PublicMode>publicModePair,JsonObject jsonObject){
        var totalObj = new JsonObject();

        var total = publicModePair.getLeft().getTotalValues();
        if(total == null){
            jsonObject.add("totalData",totalObj);
            return;
        }

        totalObj.addProperty("calcProducedKWH",getIfOverZero(total.getCalcProducedKWH()));
        totalObj.addProperty("producedKWH",getIfOverZero(total.getProducedKWH()));

        if(publicModePair.getRight() == null || publicModePair.getRight() == PublicMode.ALL){//public or owner access
            totalObj.addProperty("calcConsumedKWH",getIfOverZero(total.getCalcConsumedKWH()));
            totalObj.addProperty("consumedKWH",getIfOverZero(total.getConsumedKWH()));
        }

        if(publicModePair.getRight() == null){//owner access
            totalObj.addProperty("calcProducedKWHPrice",getIfOverZero(total.getCalcProducedKWHPrice()));
            totalObj.addProperty("producedKWHPrice",getIfOverZero(total.getProducedKWHPrice()));
            totalObj.addProperty("calcConsumedKWHPrice",getIfOverZero(total.getCalcConsumedKWHPrice()));
            totalObj.addProperty("consumedKWHPrice",getIfOverZero(total.getConsumedKWHPrice()));
        }else if(publicModePair.getLeft().getViewData().getTotalPricingPublicOverride() == Boolean.TRUE){
            totalObj.addProperty("calcProducedKWHPrice",getIfOverZero(total.getCalcProducedKWHPrice()));
            totalObj.addProperty("producedKWHPrice",getIfOverZero(total.getProducedKWHPrice()));
        }

        var lastDayRes = influxService.getLastDayDataAsJson(publicModePair.getLeft());
        var resMap = extractSingleResult(lastDayRes);

        addIfPresent(totalObj,resMap,"CalcProducedKWH","calcProducedKWHDay");
        addIfPresent(totalObj,resMap,"ProducedKWH","producedKWHDay");

        if(publicModePair.getRight() == null || publicModePair.getRight() == PublicMode.ALL){//public or owner access
            addIfPresent(totalObj,resMap,"CalcConsumedKWH","calcConsumedKWHDay");
            addIfPresent(totalObj,resMap,"ConsumedKWH","consumedKWHDay");
        }
        if(publicModePair.getRight() == null){//owner acces
            addIfPresent(totalObj,resMap,"CalcProducedKWHPrice","calcProducedKWHPriceDay");
            addIfPresent(totalObj,resMap,"ProducedKWHPrice","producedKWHPriceDay");
            addIfPresent(totalObj,resMap,"CalcConsumedKWHPrice","calcConsumedKWHPriceDay");
            addIfPresent(totalObj,resMap,"ConsumedKWHPrice","consumedKWHPriceDay");
        }else if(publicModePair.getLeft().getViewData().getTotalPricingPublicOverride() == Boolean.TRUE){
            addIfPresent(totalObj,resMap,"CalcProducedKWHPrice","calcProducedKWHPriceDay");
            addIfPresent(totalObj,resMap,"ProducedKWHPrice","producedKWHPriceDay");
        }

        jsonObject.add("totalData",totalObj);
    }

    @GetMapping("/all")
    public String getAllData(@RequestParam String systemId, @RequestParam Long from,@RequestParam Long to){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallAll();

        var pairIdPublic = solarSystemService.findSolarSystemByWithAccess(systemId);

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var fluxResult = influxService.getAllDataAsJson(pairIdPublic.getLeft(),fromDate, toDate,pairIdPublic.getRight() == PublicMode.PRODUCTION);
        var res = convertToResult(fluxResult,true);
        totalValuesToJsonObject(pairIdPublic,res);
        var realRes = res.toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return realRes;
    }



    @GetMapping("/latest")
    public String getLast5Min(@RequestParam String systemId,@RequestParam long duration){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallLatest();

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var pairIdPublic = solarSystemService.findSolarSystemByWithAccess(systemId);

        var fluxResult = influxService.getLastFiveMin(pairIdPublic.getLeft(),duration,pairIdPublic.getRight() == PublicMode.PRODUCTION);
        var res = convertToResult(fluxResult,true);
        totalValuesToJsonObject(pairIdPublic,res);

        var finalRes = res.toString();
        
        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return finalRes;
    }

    @GetMapping("/statistics/all")
    public String getProduceStats(@RequestParam String systemId, @RequestParam Long from,@RequestParam Long to){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallStatisticAll();

        var pairIdPublic = solarSystemService.findSolarSystemByWithAccess(systemId);
        var fluxResult = influxService.getStatisticsDataAsJson(pairIdPublic.getLeft(),  new Date(from), new Date(to),pairIdPublic.getRight() == PublicMode.PRODUCTION);
        var res = convertToStatisticResult(fluxResult).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }

    @GetMapping("/statistics/latest")
    public String getProduceStatsLatest(@RequestParam String systemId){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallStatisticLatest();

        var pairIdPublic = solarSystemService.findSolarSystemByWithAccess(systemId);
        var fluxResult = influxService.getlastTwoDaysStatistic(pairIdPublic.getLeft(),pairIdPublic.getRight() == PublicMode.PRODUCTION);
        var res = convertToStatisticResult(fluxResult).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }

    private Map<String,Integer> mapAndValidateCombinedIds(String[] ids){

        if(ids.length < 1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"At least two ids must be specified");
        }

        if(ids.length > 10){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"More the 10 ids are not allow on same view");
        }

        Map<String,Integer> mappedIds = new HashMap<>();
        int i=0;
        for (String id : ids) {
            mappedIds.put(id,i);
            i++;
        }
        return mappedIds;
    }

    @GetMapping("/combined/all")
    public String getAllDataCombined(@RequestParam("SystemIds") String[] ids, @RequestParam Long from,@RequestParam Long to){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallMultAll();

        Date fromDate = new Date(from);
        Date toDate =  new Date(to);
        validateTimeRange(fromDate,toDate);

        var mappedIds = mapAndValidateCombinedIds(ids);

        var publicPairs = solarSystemService.findSolarSystemsByWithAccess(Arrays.stream(ids).toList()).stream()
                .map(v->new ImmutablePair<SolarSystem,Boolean>(v.getLeft(),v.getRight() == PublicMode.PRODUCTION))
                .collect(Collectors.toList());
        var fluxResult = influxService.getProductionCombined(publicPairs,fromDate,toDate,mappedIds);
        var res = convertToResult(fluxResult,false).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }


    @GetMapping("/combined/latest")
    public String getAllDataCombined(@RequestParam("SystemIds") String[] ids,@RequestParam long duration){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallMultLatest();

        if(duration <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid duration");
        }

        var mappedIds = mapAndValidateCombinedIds(ids);

        var publicPairs = solarSystemService.findSolarSystemsByWithAccess(Arrays.stream(ids).toList()).stream()
                .map(v->new ImmutablePair<SolarSystem,Boolean>(v.getLeft(),v.getRight() == PublicMode.PRODUCTION))
                .collect(Collectors.toList());
        var fluxResult = influxService.getLastFiveMinutesCombined(publicPairs,duration,mappedIds);
        var res = convertToResult(fluxResult,false).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }

    /*
    @GetMapping("/combined/latest")
    public String getCombinedLast5Min(@RequestParam String systemId){
        return "{}";
    }
    */


    @GetMapping("/combined/statistics/all")
    public String getProduceStats(@RequestParam("SystemIds") String[] ids, @RequestParam Long from,@RequestParam Long to){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallMultStatisticAll();

        var mappedIds = mapAndValidateCombinedIds(ids);
        var publicPairs = solarSystemService.findSolarSystemsByWithAccess(Arrays.stream(ids).toList()).stream()
                .map(v->new ImmutablePair<SolarSystem,Boolean>(v.getLeft(),v.getRight() == PublicMode.PRODUCTION))
                .collect(Collectors.toList());
        var fluxResult = influxService.getCombinedStatisticsDataAsJson(publicPairs,  new Date(from), new Date(to),mappedIds);
        var res = convertToCombinedStatisticResult(fluxResult).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }


    @GetMapping("/combined/statistics/latest")
    public String getProduceStatsLatest(@RequestParam("SystemIds") String[] ids){

        apiMeterRegistry.incrementApiClientCall();
        apiMeterRegistry.incrementApiClientCallMultStatisticLatest();

        var mappedIds = mapAndValidateCombinedIds(ids);
        var publicPairs = solarSystemService.findSolarSystemsByWithAccess(Arrays.stream(ids).toList()).stream()
                .map(v->new ImmutablePair<SolarSystem,Boolean>(v.getLeft(),v.getRight() == PublicMode.PRODUCTION))
                .collect(Collectors.toList());
        var fluxResult = influxService.getLastCombinedStatisticsDataAsJson(publicPairs,mappedIds);
        var res = convertToCombinedStatisticResult(fluxResult).toString();

        apiMeterRegistry.incrementApiClientCallSuccessFul();

        return res;
    }

}
