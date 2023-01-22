package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.dtos.status.BooleanStatusTDO;
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

    public BooleanStatusTDO addStatus(String name, boolean value, long solarSystemId, long userId){

        var res = getStatus(userId,solarSystemId,name);
        if(!res.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status with this name already exists");
        }

        return setStatus(name,value,solarSystemId,userId,false);
    }

    public BooleanStatusTDO setStatus(String name, boolean value, long solarSystemId, long userId){
        return setStatus(name,value,solarSystemId,userId,true);
    }

    public BooleanStatusTDO setStatus(String name, boolean value, long solarSystemId, long userId,boolean withCheck){

        if(withCheck) {
            //check if value exits
            var res = getStatus(userId, solarSystemId, name);
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
                .addTag("system", ""+solarSystemId)
                .addTag("active", "1");

        influxConnection.writePointForUser(userId,point);

        return BooleanStatusTDO.builder()
            .name(name)
            .value(value)
            .lastSet(now)
            .build();
    }

    public void removeStatus(String name,long solarSystemId,long userId){

        var map = new HashMap<String,Object>();
        map.put(name,false);
        var point = Point.measurement(CUSTOM_STATUS_BOOLEAN.getName())
                .time(new Date().getTime(), WritePrecision.MS)
                .addFields(map)
                .addTag("system", ""+solarSystemId)
                .addTag("active", "0");

        influxConnection.writePointForUser(userId,point);
    }


    public List<FluxTable> getStatus(long userId, long systemId) {
        var query = "from(bucket: \"user-" + userId + "\")\n" +
                "  |> range(start: 0, stop: now())\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + CUSTOM_STATUS_BOOLEAN + "\")\n" +
                "  |> filter(fn: (r) => r[\"system\"] == \"" + systemId + "\")\n" +
                "  |> group(columns: [\"_field\"])\n" +
                "  |> last()\n" +
                "  |> filter(fn: (r) => r[\"active\"] == \"1\")";

        return influxConnection.getClient().getQueryApi().query(query);
    }

    public List<FluxTable> getStatus(long userId, long systemId,String name) {
        var query = "from(bucket: \"user-" + userId + "\")\n" +
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
