package de.tostsoft.solarmonitoring.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxInputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxOutputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxPoint;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import okhttp3.OkHttpClient;
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

  private InfluxDBClient influxDBClient;
  public InfluxDBClient getClient() {
    return influxDBClient;
  }

  private boolean isSolarTypeImplemented(SolarSystemType type){
    return type == SolarSystemType.SELFMADE ||
        type == SolarSystemType.SELFMADE_DEVICE ||
        type == SolarSystemType.SELFMADE_CONSUMPTION ||
        type == SolarSystemType.SELFMADE_INVERTER ||
        type == SolarSystemType.SIMPLE ||
        type == SolarSystemType.VERY_SIMPLE ||
        type == SolarSystemType.GRID;
  }

  private boolean isFunctionIgnored(Method m){
    return (!m.getName().startsWith("get") ||
        m.getName().equals("getClass") ||
        m.getName().equals("getType") ||
        m.getName().equals("getTimestamp") ||
        m.getName().equals("getSystemId") ||
        m.getName().equals("getDeviceId") ||
        m.getName().equals("getId"));
  }


  @PostConstruct
  void init() {
    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient().newBuilder()
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS);

    InfluxDBClientOptions options = InfluxDBClientOptions.builder()
            .url(influxUrl)
            .authenticateToken(influxToken.toCharArray())
            .org(influxOrganisation)
            .okHttpClient(okHttpClientBuilder)
            .build();

    influxDBClient = InfluxDBClientFactory.create(options);
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
    newPoints(solarSystem, Collections.singletonList(solarData));
  }

  public void newPoints(SolarSystem solarSystem,List<GenericInfluxPoint> solarDatas) {
    for (GenericInfluxPoint solarData : solarDatas) {
      if(!isSolarTypeImplemented(solarData.getType())){
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
      }
    }
    //TODO find out if new creation of this ist best way to do it
    var localInfluxClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation, "user-"+solarSystem.getRelationOwnedBy().getId());
    WriteApiBlocking writeApi = localInfluxClient.getWriteApiBlocking();

    var points = new ArrayList<Point>();

    for (GenericInfluxPoint solarData : solarDatas) {

      Method[] methods = solarData.getClass().getMethods();
      Map<String, Object> map = new HashMap<String, Object>();
      for (Method m : methods) {
        if (isFunctionIgnored(m)) {
          continue;
        }
        try {
          Object o = m.invoke(solarData);

          if (o == null) {
            continue;
          }
          map.put(m.getName().substring(3), o);
        } catch (Exception ex) {
          LOG.error("error while saving datapoint",ex);
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while saving datapoint");
        }
      }

      var additionalTags = new HashMap<String,String>();

      String mesurement = null;
      if (solarData.getType() == SolarSystemType.SELFMADE || solarData.getType() == SolarSystemType.SELFMADE_DEVICE
          || solarData.getType() == SolarSystemType.SELFMADE_CONSUMPTION
          || solarData.getType() == SolarSystemType.SELFMADE_INVERTER) {
        mesurement = "selfmade-solar-data";
      }

      if (solarData.getType() == SolarSystemType.SIMPLE || solarData.getType() == SolarSystemType.VERY_SIMPLE) {
        mesurement = "simple-solar-data";
      }

      if (solarData.getType() == SolarSystemType.GRID) {
        if(solarData instanceof GridSolarInfluxInputPoint){
          mesurement = "grid-solar-data-input";
          var input = (GridSolarInfluxInputPoint)solarData;
          additionalTags.put("deviceId",""+input.getDeviceId());
          additionalTags.put("id",""+input.getId());
        }else if(solarData instanceof GridSolarInfluxOutputPoint){
          mesurement = "grid-solar-data-output";
          var output = (GridSolarInfluxOutputPoint)solarData;
          additionalTags.put("deviceId",""+output.getDeviceId());
          additionalTags.put("id",""+output.getId());
        }else if(solarData instanceof GridSolarInfluxPoint){
          mesurement = "grid-solar-data";
          var gridPoint = (GridSolarInfluxPoint)solarData;
          Long id = gridPoint.getId();
          if(id == null){
            id = 0L;
          }
          additionalTags.put("id",""+id);
        }else{
          LOG.error("error while saving datapoint unkown data class {}",solarData.getClass());
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while saving datapoint");
        }
      }

      var point = Point.measurement(mesurement)
          .time(solarData.getTimestamp(), WritePrecision.MS)
          .addFields(map)
          .addTag("type", solarData.getType().toString())
          .addTag("system", ""+solarData.getSystemId())
          .addTags(additionalTags);

      LOG.debug("generated Data Point {} for system {}", points,solarSystem.getId());

      points.add(point);
    }

    writeApi.writePoints(points);
    LOG.info("wrote Data Points {}", points.size());
    localInfluxClient.close();
  }

}

