package de.tostsoft.solarmonitoring.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.exception.InternalServerException;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


@Service
public class InfluxConnection {
    private static final Logger LOG = LoggerFactory.getLogger(InfluxConnection.class);

    @Value("${influx.url}")
    private String influxUrl;
    @Value("${influx.token}")
    private String influxToken;
    @Value("${influx.organisation}")
    private String influxOrganisation;
    @Value("${influx.bucket}")
    private String influxBucket;

    private InfluxDBClient influxDBClient;

    public InfluxDBClient getClient() {
        return influxDBClient;
    }

    @PostConstruct
    void init() {
        influxDBClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation, influxBucket);
    }

    public void newPoint(GenericInfluxPoint solarData, String token) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        Method[] methods = solarData.getClass().getMethods();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Method m : methods) {
            if (!m.getName().startsWith("get") || m.getName().equals("getClass") || m.getName().equals("getType") || m.getName().equals("getTimestamp")) {
                continue;
            }
            try {
                Object o = m.invoke(solarData);

                if (o == null) {
                    continue;
                }
                map.put(m.getName().substring(3), o);
            } catch (Exception e) {
                throw new InternalServerException("Method not found");
            }
        }
        Point point = Point.measurement(solarData.getType().toString())
                .time(solarData.getTimestamp(), WritePrecision.MS)
                .addFields(map).addTag("token", token);

        writeApi.writePoint(point);
        LOG.info("wrote Data Point {}", solarData);
    }

}

