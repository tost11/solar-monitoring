package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.dtos.grafana.*;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GrafanaService {

  private static final Logger LOG = LoggerFactory.getLogger(GrafanaService.class);

  //@Value("${grafana.token}")
  //private String apiToken;

  @Value("${grafana.user}")
  private String grafanaUser;

  @Value("${grafana.password}")
  private String grafanaPassword;

  @Value("${proxy.grafana.target.url}")
  private String grafanaUrl;

  private String dashboardTemplateNewSelfmadeDevice;

  @AllArgsConstructor
  @Getter
  public class GrafanaFolderData{
    public long id;
    public String folderUid;
  }

  @PostConstruct
  private void init() throws Exception{
    LOG.info("Loading grafana template files");
    File file;
    try {
      file = ResourceUtils.getFile("classpath:solar-template-selfmade-device.json");
    } catch (FileNotFoundException ignored) {
      file = ResourceUtils.getFile("solar-template-selfmade-device.json");
    }

    dashboardTemplateNewSelfmadeDevice = new String(Files.readAllBytes(file.toPath()));
  }

  private HttpHeaders createHeaders(){
    return new HttpHeaders() {{
      String auth = grafanaUser + ":" + grafanaPassword;
      byte[] encodedAuth = Base64.encodeBase64(
          auth.getBytes(Charset.defaultCharset()));
      String authHeader = "Basic " + new String(encodedAuth);
      set("Authorization", authHeader);
      set("Content-Type","application/json; charset=UTF-8");
    }};
  }

  private ResponseEntity<GrafanaCreateDashboardResponseDTO> createDashboard(String json){
    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>(json,createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/dashboards/db", HttpMethod.POST,entity, GrafanaCreateDashboardResponseDTO.class);
  }


  public GrafanaFolderData createFolder(String name,String folderUid){
    RestTemplate restTemplate = new RestTemplate();

    String json = "{\"title\": \""+name+"\""+(folderUid!=null?",\"uid\":\""+folderUid+"\"":"")+"}";

    var entity = new HttpEntity<String>(json,createHeaders());

    var resp = restTemplate.exchange(grafanaUrl+"/api/folders", HttpMethod.POST,entity, GrafanaFolderResponseDTO.class);

    return new GrafanaFolderData(resp.getBody().getId(),resp.getBody().getUid());
  }

  public ResponseEntity<GrafanaFolderResponseDTO> deleteFolder(String uid){

    RestTemplate restTemplate = new RestTemplate();


    var entity = new HttpEntity<String>("",createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/folders/"+uid, HttpMethod.DELETE,entity,GrafanaFolderResponseDTO.class);
  }
  public ResponseEntity<GrafanaFolderResponseDTO> deleteDashboard(String uid){

    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>("",createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/dashboards/uid/"+uid, HttpMethod.DELETE,entity,GrafanaFolderResponseDTO.class);
  }

  public ResponseEntity<GrafanaFoldersDTO[]> getFolders(){
    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>(createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/folders", HttpMethod.GET,entity, GrafanaFoldersDTO[].class);
  }

  public void setPermissionsForFolder(long userId,String folderUid){
    RestTemplate restTemplate = new RestTemplate();

    String json = "{\"items\": [{\"role\": \"Editor\",\"permission\": 2 },{\"userId\": "+userId+",\"permission\": 1}]}";

    var entity = new HttpEntity<String>(json,createHeaders());

    restTemplate.exchange(grafanaUrl+"/api/folders/"+folderUid+"/permissions", HttpMethod.POST,entity, String.class).getStatusCode();
  }


  public long createNewUser(String login,String name) {

    RestTemplate restTemplate = new RestTemplate();

    LOG.info("generate new grafana user");
    String json =
        "{\"name\":\"" + name + "\",\"email\":\"" + login + "@localhost\",\"login\":\"" + login + "\",\"password\":\""
            + UUID.randomUUID() + "\"}";
    var entity = new HttpEntity<String>(json, createHeaders());

    var userResp = restTemplate.exchange(grafanaUrl + "/api/admin/users", HttpMethod.POST, entity,
        GrafanaCreateUserDTO.class);

    return userResp.getBody().getId();

  }

  public boolean doseUserExist(String username){

    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>(createHeaders());
    try {
      restTemplate.exchange(grafanaUrl+"/api/users/lookup?loginOrEmail="+username+"@localhost",HttpMethod.GET,entity, String.class);

    } catch (RestClientException e) {
      return false;
    }

    return true;
  }

  public void deleteUser(long userID){

    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>(createHeaders());

    restTemplate.exchange(grafanaUrl+"/api/admin/users/"+userID,HttpMethod.DELETE,entity, String.class);
  }

  public List<GrafanaUserDTO> getGrafanaUsers(long page,long size){
    RestTemplate restTemplate = new RestTemplate();
    var entity = new HttpEntity<String>(createHeaders());
    return Arrays.asList(restTemplate.exchange(grafanaUrl+"/api/users?perpage="+size+"&page="+page,HttpMethod.GET,entity, GrafanaUserDTO[].class).getBody());
  }

  public GrafanaCreateDashboardResponseDTO createNewSelfmadeDeviceSolarDashboard(SolarSystem system){

    String json;

    if(system.getType() == SolarSystemType.SELFMADE || system.getType() == SolarSystemType.SELFMADE_DEVICE || system.getType() == SolarSystemType.SELFMADE_INVERTER || system.getType() == SolarSystemType.SELFMADE_CONSUMPTION) {
      json = dashboardTemplateNewSelfmadeDevice;
      json = StringUtils.replace(json, "__TEMP_BUCKET__", "user-"+system.getRelationOwnedBy().getId());
      json = StringUtils.replace(json, "__TEMP_ID__", ""+system.getId());
      json = StringUtils.replace(json, "__DASHBOARD_TITLE__", "dashboard-"+system.getId());
      json = StringUtils.replace(json, "\"uid\": null", "\"uid\": \"" + "dashboard-"+system.getId() + "\"");
      if(system.getBatteryVoltage()!=null){
        ///TODO change the Battery Voltage
      }
      if(system.getInverterVoltage()!=null){
        ///TODO change the Inverter Voltage
      }
      json = "{\"dashboard\":" + json + ",\"folderUid\":\"" + "user-"+system.getRelationOwnedBy().getId() + "\",\"overwrite\": true}";
    }else{
      throw new NotImplementedException("For Solar type: "+system.getType()+" no dashboard json is available");
    }
    var resp = createDashboard(json);
    if(resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null){
      LOG.error("Error while creating dashboard response is not 200 "+resp.toString());
      return null;
    }
    if(StringUtils.compareIgnoreCase(resp.getBody().getStatus(),"success") != 0){
      LOG.error("Error while creating dashboard response is not 200 "+resp.toString());
      return null;
    }

    //will be set by parent folder not more needet for now
    //var permResp = setDefaultPermissionsOnDashboard(resp.getBody().getId(),userId);
    //if(permResp.getStatusCode() != HttpStatus.OK){
    //  return null;
    //}

    return resp.getBody();
  }
}
