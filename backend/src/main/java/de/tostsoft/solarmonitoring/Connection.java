package de.tostsoft.solarmonitoring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.module.Generic_solar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class Connection {
    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);

    InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", "influx-password-123!".toCharArray(), "my-org", "my-bucket");


    public void newPoint(Generic_solar generic_solar) throws Exception{
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        Method[] methods = Generic_solar.class.getMethods();
        Map<String, Object> map = new HashMap<String, Object>();
        for(Method m : methods) {
            if(!m.getName().startsWith("get")||m.getName().equals("getClass")){
                continue;
            }
            Object o = m.invoke(generic_solar);
            if(o==null){
                continue;
            }
            map.put(m.getName().substring(3),o);
        }
        Point point = Point.measurement("GenericsSolar")
                .time(generic_solar.getTimestamp(), WritePrecision.MS)
                .addFields(map);

        writeApi.writePoint(point);
        LOG.info("wrote Data Point {}",generic_solar);
    }

}

