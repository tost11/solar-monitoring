package de.tostsoft.solarmonitoring.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.OrganizationsQuery;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.influx.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZonedDateTime;
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

  private String organizationId;

  public String getOrganizaionId(){
    return organizationId;
  }

  @Autowired
  UserRepository userRepository;

  private InfluxDBClient influxDBClient;
  public InfluxDBClient getClient() {
    return influxDBClient;
  }

  private boolean isFunctionIgnored(Method m){
    return (!m.getName().startsWith("get") ||
        m.getName().equals("getClass") ||
        m.getName().equals("getType") ||
        m.getName().equals("getTimestamp") ||
        m.getName().equals("getSystemId") ||
        m.getName().equals("getDeviceId") ||
        m.getName().equals("getMeasurement") ||
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

    var oq = new OrganizationsQuery();
    oq.setOrg("my-org");
    organizationId = getClient().getOrganizationsApi().findOrganizations(oq).get(0).getId();
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

  public Instant getFirstDataEver(SolarSystem solarSystem){
    String query = "from(bucket: \"user-"+solarSystem.getRelationOwnedBy().getId()+"\")\n"
        + "  |> range(start: 0, stop: now())\n"
        + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+ InfluxMeasurement.SOLAR_DATA+ "\")\n"
        + "  |> filter(fn: (r) => r[\"system\"] == \""+solarSystem.getId()+"\")\n"
        + "  |> first()\n";

    var res = influxDBClient.getQueryApi().query(query);
    if(res.isEmpty() || res.get(0).getRecords().isEmpty()){
      return null;
    }
    return res.get(0).getRecords().get(0).getTime();
  }

  public void newPoint(SolarSystem solarSystem, GenericInfluxPoint solarData) {
    newPoints(solarSystem, Collections.singletonList(solarData));
  }

  public void newPoints(SolarSystem solarSystem,List<GenericInfluxPoint> solarDatas) {
    for (GenericInfluxPoint solarData : solarDatas) {
      solarData.setType(solarSystem.getType());
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

      String mesurement = solarData.getMeasurement().toString();

      if (solarData instanceof SolarDeviceInfluxPoint) {
        var impl = (SolarDeviceInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
      }else if (solarData instanceof SolarInInputDCInfluxPoint) {
        var impl = (SolarInInputDCInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      } else if (solarData instanceof SolarInInputACInfluxPoint) {
        var impl = (SolarInInputACInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      } else if (solarData instanceof SolarOutputDCInfluxPoint) {
        var impl = (SolarOutputDCInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      }  else if (solarData instanceof SolarOutputACInfluxPoint) {
        var impl = (SolarOutputACInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      }  else if (solarData instanceof SolarBatteryInfluxPoint) {
        var impl = (SolarBatteryInfluxPoint)solarData;
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
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

