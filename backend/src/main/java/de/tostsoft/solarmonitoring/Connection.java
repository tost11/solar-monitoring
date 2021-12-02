package de.tostsoft.solarmonitoring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class Connection {
    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);

    InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", "influx-password-123!".toCharArray(), "my-org", "my-bucket");


    public void newPoint(GenericInfluxPoint solarData,String token) throws Exception{
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();


        Method[] methods = solarData.getClass().getMethods();
        Map<String, Object> map = new HashMap<String, Object>();
        for(Method m : methods) {
            if(!m.getName().startsWith("get")||m.getName().equals("getClass")||m.getName().equals("getMeasurement")||m.getName().equals("getTimeStep")){
                continue;
            }
            Object o = m.invoke(solarData);
            if(o==null){
                continue;
            }
            map.put(m.getName().substring(3),o);
        }
        Point point = Point.measurement(solarData.getMeasurement().toString())
                .time(solarData.getTimeStep(), WritePrecision.MS)
                .addFields(map).addTag("token",token);

        writeApi.writePoint(point);
        LOG.info("wrote Data Point {}",solarData);
    }

}

