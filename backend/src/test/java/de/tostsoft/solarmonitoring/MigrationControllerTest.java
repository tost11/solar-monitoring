package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.GrafanaService;
import de.tostsoft.solarmonitoring.service.UserService;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class MigrationControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationControllerTest.class);

    @LocalServerPort
    int randomServerPort;
    @Value("${grafana.user}")
    private String grafanaUser;

    @Value("${grafana.password}")
    private String grafanaPassword;

    @Value("${proxy.grafana.target.url}")
    private String grafanaUrl;
    @Autowired
    private UserService userService;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private TestRestTemplate restTemplate;

    HttpHeaders headers = new HttpHeaders();



    @BeforeEach
    public void init(){
        cleanUPData();
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
    private void cleanUPData(){
        String json = "";
        var entity = new HttpEntity<String>(json,createHeaders());
        var list= grafanaService.getFolders();
        for (int i=0;list.getBody().length>i;i++){
            var foldersDTO =  list.getBody()[i];
            grafanaService.deleteFolder(foldersDTO.getUid());
        }

        var userList =restTemplate.exchange(grafanaUrl+"/api/users",HttpMethod.GET,entity, GrafanaUserDTO[].class);
        LOG.info("list of User "+userList.toString());
        for (int i=0;userList.getBody().length>i;i++){
            var grafanaUser =  userList.getBody()[i];
            if (grafanaUser.getLogin().equals("admin")) {
                continue;
            }
            LOG.info("Delete Influx bucket");
            try {
                influxConnection.deleteBucket(grafanaUser.getLogin());
            }catch (Exception e){
                LOG.error(e.toString());
            }
            LOG.info("Grafana User Delete"+grafanaUser.toString());
            grafanaService.deleteUser(grafanaUser.getId());
        }
        solarSystemRepository.deleteAll();
        userRepository.deleteAll();
    }
    private SolarSystemDTO creatUserAndSystem(SolarSystemType solarSystemType) {
        UserRegisterDTO user = new UserRegisterDTO("testLogin", "testtest");
        userService.registerUser(user);
        RegisterSolarSystemDTO registerSolarSystemDTO =new RegisterSolarSystemDTO("testSystem",solarSystemType);
        HttpEntity httpEntity = new HttpEntity(user);
        ResponseEntity<UserDTO> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity, UserDTO.class);

        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+response.getBody().getJwt());
        httpEntity = new HttpEntity(registerSolarSystemDTO,headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);

        return responseSystem.getBody();
    }

    @Test
    public void migrate_MigrateDTO_OK(){
        SolarSystemDTO solarSystemDTO = creatUserAndSystem(SolarSystemType.SELFMADE);
        MigrationDTO migrationDTO=new MigrationDTO(solarSystemDTO.getType());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(migrationDTO,headers);
        ResponseEntity<String> result= restTemplate.exchange("http://localhost:" + randomServerPort + "/api/migration", HttpMethod.POST, httpEntity,String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

    }
}
