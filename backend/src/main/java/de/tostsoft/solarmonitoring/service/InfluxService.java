package de.tostsoft.solarmonitoring.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class InfluxService {
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    public String getAllDataAsJson(String query) {
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

    public String getStatisticDataAsJson(String query) {
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
