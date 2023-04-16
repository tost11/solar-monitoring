package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement.CUSTOM_STATUS_BOOLEAN;

@Service
public class StatusService {

    @Autowired
    private InfluxConnection influxConnection;

    public BooleanStatusTDO addStatus(String name, boolean value,  SolarSystem solarSystem){

        var res = getStatus(solarSystem.getOwnedBy().getInfluxBucketName(),solarSystem.getInfluxTagName(),name);
        if(!res.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status with this name already exists");
        }

        return setStatus(name,value,solarSystem,false);
    }

    public BooleanStatusTDO setStatus(String name, boolean value, SolarSystem solarSystem){
        return setStatus(name,value,solarSystem,true);
    }

    public BooleanStatusTDO setStatus(String name, boolean value, SolarSystem solarSystem,boolean withCheck){

        if(withCheck) {
            //check if value exits
            var res = getStatus(solarSystem.getOwnedBy().getInfluxBucketName(), solarSystem.getInfluxTagName(), name);
            if (res.isEmpty() || res.get(0).getRecords().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status with this name dose not exits");
            }
        }

        ZonedDateTime now = ZonedDateTime.now();

        var map = new HashMap<String,Object>();
        map.put(name,value);

        var point = Point.measurement(CUSTOM_STATUS_BOOLEAN.getName())
                .time(now.toInstant().toEpochMilli(), WritePrecision.MS)
                .addFields(map)
                .addTag("system", solarSystem.getInfluxTagName())
                .addTag("active", "1");

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);

        return BooleanStatusTDO.builder()
            .name(name)
            .value(value)
            .lastSet(now)
            .build();
    }

    public void removeStatus(String name,SolarSystem solarSystem){

        var map = new HashMap<String,Object>();
        map.put(name,false);
        var point = Point.measurement(CUSTOM_STATUS_BOOLEAN.getName())
            .time(new Date().getTime(), WritePrecision.MS)
            .addFields(map)
            .addTag("system", solarSystem.getInfluxTagName())
            .addTag("active", "0");

        influxConnection.writePointForUser(solarSystem.getOwnedBy().getInfluxBucketName(),point);
    }


    private void removeStatus(String name,String solarSystemId,String bucketName){

        var map = new HashMap<String,Object>();
        map.put(name,false);
        var point = Point.measurement(CUSTOM_STATUS_BOOLEAN.getName())
                .time(new Date().getTime(), WritePrecision.MS)
                .addFields(map)
                .addTag("system", solarSystemId)
                .addTag("active", "0");

        influxConnection.writePointForUser(bucketName,point);
    }

    public List<FluxTable> getStatus(String bucketName, String systemId) {
        var query = "from(bucket: \"" + bucketName + "\")\n" +
                "  |> range(start: 0, stop: now())\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + CUSTOM_STATUS_BOOLEAN + "\")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
                "  |> group(columns: [\"_field\"])\n" +
                "  |> last()\n" +
                "  |> filter(fn: (r) => r[\"active\"] == \"1\")";

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getStatus(String bucketName, String systemId,String name) {
        var query = "from(bucket: \"" + bucketName + "\")\n" +
                "  |> range(start: 0, stop: now())\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + CUSTOM_STATUS_BOOLEAN + "\")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"" + InfluxConnection.escapeString(name) + "\")\n" +
                "  |> group(columns: [\"_field\"])\n" +
                "  |> last()\n" +
                "  |> filter(fn: (r) => r[\"active\"] == \"1\")";

        return influxConnection.getClient().getQueryApi().query(query);
    }
}
