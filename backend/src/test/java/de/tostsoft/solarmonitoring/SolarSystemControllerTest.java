package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.GrafanaService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolarSystemControllerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemControllerTest.class);

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
    @Autowired
    private SolarSystemService solarSystemService;

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
    private void cleanUPData() {
        String json = "";
        var entity = new HttpEntity<String>(json, createHeaders());
        try {
            var list = grafanaService.getFolders();
            for (int i = 0; list.getBody().length > i; i++) {
                var foldersDTO = list.getBody()[i];
                grafanaService.deleteFolder(foldersDTO.getUid());
            }
        }catch (Exception e){
            System.out.println("no Folder in Database");
        }

        try {
            var userList =restTemplate.exchange(grafanaUrl+"/api/users", HttpMethod.GET,entity, GrafanaUserDTO[].class);
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
        } catch (Exception e){
            System.out.println("no user in grafana");

        }


    }
    private UserDTO newUser(){
        UserRegisterDTO userRegisterDTO= new UserRegisterDTO("testRegister","testtest");
        HttpEntity httpEntity = new HttpEntity(userRegisterDTO);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, UserDTO.class);
        return result.getBody();
    }
    private SolarSystemDTO addNewSolarSystem(UserDTO userDTO){

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+userDTO.getJwt());
        RegisterSolarSystemDTO registerSolarSystemDTO = new RegisterSolarSystemDTO("testSystem", SolarSystemType.SELFMADE);
        HttpEntity httpEntity = new HttpEntity(registerSolarSystemDTO,headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);
        return responseSystem.getBody();

    }

    @Test
    public void newSolar_RegisterSolarSystemDTO_OK(){
        UserDTO userDTO = newUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+userDTO.getJwt());
        RegisterSolarSystemDTO registerSolarSystemDTO = new RegisterSolarSystemDTO("testSystem", SolarSystemType.SELFMADE);
        HttpEntity httpEntity = new HttpEntity(registerSolarSystemDTO,headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);
        assertThat(solarSystemRepository.existsAllByToken(responseSystem.getBody().getToken())).isTrue();
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    public void getSolar_systemID_OK(){
        UserDTO userDTO= newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem( userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<GettingSolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/"+(solarSystemDTO.getId()), HttpMethod.GET, httpEntity, GettingSolarSystemDTO.class);
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSystem.getBody().getName()).isEqualTo(solarSystemDTO.getName());
        assertThat(responseSystem.getBody().getCreationDate()).isEqualTo(solarSystemDTO.getCreationDate());
        assertThat(responseSystem.getBody().getType()).isEqualTo(solarSystemDTO.getType());
    }
    @Test
    public void getSystems__OK(){
        UserDTO userDTO= newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem( userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<GettingSolarSystemDTO[]> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/all", HttpMethod.GET, httpEntity, GettingSolarSystemDTO[].class);
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSystem.getBody()[0].getName()).isEqualTo(solarSystemDTO.getName());
        assertThat(responseSystem.getBody()[0].getCreationDate()).isEqualTo(solarSystemDTO.getCreationDate());
        assertThat(responseSystem.getBody()[0].getType()).isEqualTo(solarSystemDTO.getType());
    }
    @Test
    public void deleteSystem_systemToken_OK(){
        UserDTO userDTO= newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem( userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie","jwt="+userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/"+solarSystemDTO.getToken(), HttpMethod.DELETE, httpEntity, String.class);
        assertThat(solarSystemRepository.existsByName(solarSystemDTO.getName())).isFalse();
    }

}
