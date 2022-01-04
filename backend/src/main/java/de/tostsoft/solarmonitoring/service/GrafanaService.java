package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.dtos.grafana.CreateDashboardResponseDTO;
import java.io.File;
import java.nio.file.Files;
import javax.annotation.PostConstruct;
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
import org.springframework.web.client.RestTemplate;

@Service
public class GrafanaService {

  private static final Logger LOG = LoggerFactory.getLogger(GrafanaService.class);

  @Value("${grafana.token")
  private String apiToken;

  @Value("${proxy.grafana.target_url}")
  private String grafanaUrl;

  private String dashboardTemplateNewSelfmadeDevice;

  @PostConstruct
  private void init() throws Exception{
    File file = ResourceUtils.getFile("classpath:solar-template-selfmade-device.json");
    dashboardTemplateNewSelfmadeDevice = new String(Files.readAllBytes(file.toPath()));
  }

  private ResponseEntity<CreateDashboardResponseDTO> createDashboard(String json){
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type","application/json; charset=UTF-8");
    var entity = new HttpEntity<String>(json,headers);

    return restTemplate.exchange(grafanaUrl+"/api/dashboards/db", HttpMethod.POST,entity, CreateDashboardResponseDTO.class);
  }

  public CreateDashboardResponseDTO createNewSelfmadeDeviceSolarDashboard(String bucket,String token){
    String json = dashboardTemplateNewSelfmadeDevice;
    json = StringUtils.replace(json,"__TMP_BUCKET__",bucket);
    json = StringUtils.replace(json,"__TEMP_TOKEN__",token);
    json = StringUtils.replace(json,"__DASHBOARD_TITLE__","solar-selfmade-device-"+token);
    json = "{\"dashboard\":"+json+"}";
    var resp = createDashboard(json);
    if(resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null){
      LOG.error("Error while creating dashboard response is not 200 "+resp.toString());
      return null;
    }
    if(StringUtils.compareIgnoreCase(resp.getBody().getStatus(),"success") != 0){
      LOG.error("Error while creating dashboard response is not 200 "+resp.toString());
      return null;
    }
    return resp.getBody();
  }

}
