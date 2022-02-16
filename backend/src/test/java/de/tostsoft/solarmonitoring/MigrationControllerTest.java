package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaFoldersDTO;
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
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MigrationControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationControllerTest.class);

    @LocalServerPort
    private int randomServerPort;
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

    private final HttpHeaders headers = new HttpHeaders();



    @BeforeEach
    public void init(){
        cleanUpData();
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
    private void cleanUpData(){
        var entity = new HttpEntity<String>("",createHeaders());
        try {
        var list= grafanaService.getFolders();
        for (GrafanaFoldersDTO foldersDTO: Objects.requireNonNull(list.getBody())){
            grafanaService.deleteFolder(foldersDTO.getUid());
        }
         }catch (Exception e){
        e.printStackTrace();
        LOG.error("no Connection to Database");
         }

        var userList =restTemplate.exchange(grafanaUrl+"/api/users",HttpMethod.GET,entity, GrafanaUserDTO[].class);
        LOG.info("list of User "+userList.toString());
        for (GrafanaUserDTO grafanaUser: Objects.requireNonNull(userList.getBody())){
            if (grafanaUser.getLogin().equals("admin")) {
                continue;
            }
            LOG.info("Delete Influx bucket");
            try {
                influxConnection.deleteBucket(grafanaUser.getLogin());
            }catch (Exception e){
                e.printStackTrace();
            }
            LOG.info("Grafana User Delete"+grafanaUser);
            grafanaService.deleteUser(grafanaUser.getId());
        }
        solarSystemRepository.deleteAll();
        userRepository.deleteAll();
    }
    private SolarSystemDTO creatUserAndSystem(SolarSystemType solarSystemType) {
        UserRegisterDTO user = new UserRegisterDTO("testLogin", "testtest");
        RegisterSolarSystemDTO registerSolarSystemDTO =new RegisterSolarSystemDTO("testSystem",solarSystemType);
        userService.registerUser(user);
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
