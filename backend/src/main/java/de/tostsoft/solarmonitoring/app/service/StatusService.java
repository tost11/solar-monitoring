package de.tostsoft.solarmonitoring.app.service;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.app.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement.CUSTOM_STATUS_BOOLEAN;

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
        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(bucketName).append("\")\n")
                .append("  |> range(start: 0, stop: now())\n")
                .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(CUSTOM_STATUS_BOOLEAN).append("\")\n")
                .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
                .append("  |> group(columns: [\"_field\"])\n")
                .append("  |> last()\n")
                .append("  |> filter(fn: (r) => r[\"active\"] == \"1\")");

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }

    public List<FluxTable> getStatus(String bucketName, String systemId,String name) {
        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(bucketName).append("\")\n")
                .append("  |> range(start: 0, stop: now())\n")
                .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"").append(CUSTOM_STATUS_BOOLEAN).append("\")\n")
                .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
                .append("  |> filter(fn: (r) => r[\"_field\"] == \"").append(InfluxConnection.escapeString(name)).append("\")\n")
                .append("  |> group(columns: [\"_field\"])\n")
                .append("  |> last()\n")
                .append("  |> filter(fn: (r) => r[\"active\"] == \"1\")");

        return influxConnection.getClient().getQueryApi().query(query.toString());
    }
}
