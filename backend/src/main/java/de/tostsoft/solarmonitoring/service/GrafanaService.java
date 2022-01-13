package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaCreateDashboardResponseDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaCreateUserDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaFolderResponseDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaFoldersDTO;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.codec.binary.Base64;
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

  @Value("${proxy.grafana.target_url}")
  private String grafanaUrl;

  private String dashboardTemplateNewSelfmadeDevice;

  public class GrafanaUserdata{
    public long id;
    public String folderUuid;
  }

  @PostConstruct
  private void init() throws Exception{
    LOG.info("Loading grafana template files");
    File file = ResourceUtils.getFile("classpath:solar-template-selfmade-device.json");
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

  private ResponseEntity<String> setDefaultPermissionsOnDashboard(int dashboardId, long userId){
    RestTemplate restTemplate = new RestTemplate();

    String json = "{\"items\": [{\"role\": \"Editor\",\"permission\": 2 },{\"userId\": "+userId+",\"permission\": 1}]}";

    var entity = new HttpEntity<String>(json,createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/dashboards/id/"+dashboardId+"/permissions", HttpMethod.POST,entity, String.class);
  }

  private ResponseEntity<GrafanaFolderResponseDTO> createFolder(String name,String folderUid){
    RestTemplate restTemplate = new RestTemplate();

    String json = "{\"title\": \""+name+"\""+(folderUid!=null?",\"uid\":\""+folderUid+"\"":"")+"}";

    var entity = new HttpEntity<String>(json,createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/folders", HttpMethod.POST,entity, GrafanaFolderResponseDTO.class);
  }

  private ResponseEntity<GrafanaFoldersDTO[]> getFolders(){
    RestTemplate restTemplate = new RestTemplate();

    var entity = new HttpEntity<String>(createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/folders", HttpMethod.GET,entity, GrafanaFoldersDTO[].class);
  }

  private ResponseEntity<String> setPermissiongsForFolder(long userId,String folderUid){
    RestTemplate restTemplate = new RestTemplate();

    String json = "{\"items\": [{\"role\": \"Editor\",\"permission\": 2 },{\"userId\": "+userId+",\"permission\": 1}]}";

    var entity = new HttpEntity<String>(json,createHeaders());

    return restTemplate.exchange(grafanaUrl+"/api/folders/"+folderUid+"/permissions", HttpMethod.POST,entity, String.class);
  }


  public GrafanaUserdata createNewUser(String username,String folderUid){

    var data = new GrafanaUserdata();

    RestTemplate restTemplate = new RestTemplate();

    LOG.info("generate new grafana user");
    String json = "{\"name\":\""+username+"\",\"email\":\""+username+"@localhost\",\"login\":\""+username+"\",\"password\":\""+ UUID.randomUUID()+"\"}";
    var entity = new HttpEntity<String>(json,createHeaders());

    var userResp = restTemplate.exchange(grafanaUrl+"/api/admin/users", HttpMethod.POST,entity, GrafanaCreateUserDTO.class);
    data.id = userResp.getBody().getId();

    LOG.info("generate folder for grafana user");
    var folderResp = createFolder(username,folderUid);
    data.folderUuid = folderResp.getBody().getUid();

    setPermissiongsForFolder(data.id,data.folderUuid);

    return data;
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

  public GrafanaCreateDashboardResponseDTO createNewSelfmadeDeviceSolarDashboard(String bucket,SolarSystemDTO solarSystemDTO,String folderUid){
    return createNewSelfmadeDeviceSolarDashboard(bucket,solarSystemDTO,folderUid,null);
  }

  public GrafanaCreateDashboardResponseDTO createNewSelfmadeDeviceSolarDashboard(String bucket, SolarSystemDTO solarSystemDTO, String folderUid, String dashboardUid){
    String json = dashboardTemplateNewSelfmadeDevice;
    json = StringUtils.replace(json,"__TMP_BUCKET__",bucket);
    json = StringUtils.replace(json,"__TEMP_TOKEN__",solarSystemDTO.getToken());
    json = StringUtils.replace(json,"__DASHBOARD_TITLE__","generated-"+solarSystemDTO.getType()+"-"+solarSystemDTO.getName());
    if(dashboardUid != null){
      json = StringUtils.replace(json,"\"uid\": null","\"uid\": \""+dashboardUid+"\"");
    }
    json = "{\"dashboard\":"+json+",\"folderUid\":\""+folderUid+"\"}";
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
