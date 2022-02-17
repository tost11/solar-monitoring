package de.tostsoft.solarmonitoring.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class InfluxConnection {

  private static final Logger LOG = LoggerFactory.getLogger(InfluxConnection.class);

  @Value("${influx.url}")
  private String influxUrl;
  @Value("${influx.token}")
  private String influxToken;
  @Value("${influx.organisation}")
  private String influxOrganisation;

  @Autowired
  UserRepository userRepository;

  //@Value("${influx.bucket}")
  //private String influxBucket;


  private InfluxDBClient influxDBClient;

  public InfluxDBClient getClient() {
    return influxDBClient;
  }

  @PostConstruct
  void init() {
    influxDBClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation);
  }
  public void deleteBucket(String name){
    Bucket deleteBucket=influxDBClient.getBucketsApi().findBucketByName(name);
    influxDBClient.getBucketsApi().deleteBucket(deleteBucket);
  }
  public List<Bucket> getBuckets(){
    return influxDBClient.getBucketsApi().findBucketsByOrgName("my-org");
  }
  public boolean doseBucketExit(String name){
    return influxDBClient.getBucketsApi().findBucketByName(name) != null;
  }

  public Bucket createNewBucket(String name){
    String orgId = influxDBClient.getOrganizationsApi().findOrganizations().stream().filter(o->o.getName().equals(influxOrganisation)).findFirst().get().getId();
    return influxDBClient.getBucketsApi().createBucket(name,orgId);
  }

  public void newPoint(SolarSystem solarSystem,GenericInfluxPoint solarData) {

    if(!(solarData.getType() == SolarSystemType.SELFMADE || solarData.getType() == SolarSystemType.SELFMADE_DEVICE || solarData.getType() == SolarSystemType.SELFMADE_CONSUMPTION || solarData.getType() == SolarSystemType.SELFMADE_INVERTER)){
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
    //TODO find out if new creation of this ist best way to do it
    var localInfluxClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation, "user-"+solarSystem.getRelationOwnedBy().getId());
    WriteApiBlocking writeApi = localInfluxClient.getWriteApiBlocking();

    Method[] methods = solarData.getClass().getMethods();
    Map<String, Object> map = new HashMap<String, Object>();
    for (Method m : methods) {
      if (!m.getName().startsWith("get") || m.getName().equals("getClass") || m.getName().equals("getType")
          || m.getName().equals("getTimestamp") || m.getName().equals("getSystemId")) {
        continue;
      }
      try {
        Object o = m.invoke(solarData);

        if (o == null) {
          continue;
        }
        map.put(m.getName().substring(3), o);
      } catch (Exception ex) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while saving datapoint", ex);
      }
    }

    String mesurement = null;
    if(solarData.getType() == SolarSystemType.SELFMADE || solarData.getType() == SolarSystemType.SELFMADE_DEVICE || solarData.getType() == SolarSystemType.SELFMADE_CONSUMPTION || solarData.getType() == SolarSystemType.SELFMADE_INVERTER){
      mesurement = "selfmade-solar-data";
    }

    Point point = Point.measurement(mesurement)
        .time(solarData.getTimestamp(), WritePrecision.MS)
        .addFields(map)
        .addTag("type", solarData.getType().toString())
        .addTag("system", ""+solarData.getSystemId());

    writeApi.writePoint(point);
    LOG.info("wrote Data Point {}", solarData);
    localInfluxClient.close();
  }

}

