package de.tostsoft.solarmonitoring.lib.repository;

import com.influxdb.client.*;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.influx.*;
import jakarta.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.*;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    ZoneId z = ZoneId.of( solarSystem.getTimezone() ) ;
    LocalDateTime today = LocalDateTime.now(z);
    today = today.plus(Duration.ofDays(1));
    var end =  today.toInstant(ZoneOffset.UTC).toEpochMilli();

    String query = "from(bucket: \""+ solarSystem.getOwnedBy().getInfluxBucketName()+"\")\n"
        + "  |> range(start: 0, stop: "+end+")\n"
        + "  |> filter(fn: (r) => r[\"_measurement\"] == \""+ InfluxMeasurement.SOLAR_DATA+ "\")\n"
        + "  |> filter(fn: (r) => r[\"system\"] == \""+ solarSystem.getInfluxTagName()+"\")\n"
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

  public void writePointForUser(String bucketName,Point point){
    var localInfluxClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation, bucketName);
    WriteApiBlocking writeApi = localInfluxClient.getWriteApiBlocking();
    writeApi.writePoint(point);
    localInfluxClient.close();
  }

  static public String escapeString(String string){
    var res = string.replace("\\","\\\\");
    res = res.replace("\"","\\\"");
    return res;
  }

  public SolarInfluxPoint newPoints(SolarSystem system,List<GenericInfluxPoint> solarDatas) {

    SolarInfluxPoint last = null; //only current value of new sample filter if newer will be done later

    for (GenericInfluxPoint solarData : solarDatas) {
      solarData.setType(system.getType());
    }
    //TODO find out if new creation of this ist best way to do it
    var localInfluxClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrganisation, system.getOwnedBy().getInfluxBucketName());
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
          throw new RuntimeException("Error while saving datapoint");
        }
      }

      var additionalTags = new HashMap<String,String>();

      String mesurement = solarData.getMeasurement().toString();

      if (solarData instanceof SolarDeviceInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
      }else if (solarData instanceof SolarInInputDCInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      } else if (solarData instanceof SolarInInputACInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      } else if (solarData instanceof SolarOutputDCInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      }  else if (solarData instanceof SolarOutputACInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      }  else if (solarData instanceof SolarBatteryInfluxPoint impl) {
        additionalTags.put("id",""+impl.getId());
        additionalTags.put("deviceId",""+impl.getDeviceId());
      }else if(solarData instanceof SolarInfluxPoint impl){
        if(last == null || impl.getTimestamp() >= last.getTimestamp()){
          last = impl;
        }
      }

      var point = Point.measurement(mesurement)
          .time(solarData.getTimestamp(), WritePrecision.MS)
          .addFields(map)
          .addTag("type", solarData.getType().toString())
          .addTag("system", system.getInfluxTagName())
          .addTags(additionalTags);

      LOG.debug("generated Data Point {} for system {}", points, system.getId());

      points.add(point);
    }

    writeApi.writePoints(points);
    LOG.info("wrote Data {} Points on system {}", points.size(),system.getId());
    localInfluxClient.close();

    return last;
  }

}

