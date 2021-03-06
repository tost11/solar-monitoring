package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.UserService;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolarSystemControllerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemControllerTest.class);

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
    public void init() {
        cleanUpData();
    }

    private void cleanUpData() {

        //TODO fix that
        /*
        LOG.info("Delete Influx bucket");
        try {
            influxConnection.deleteBucket(grafanaUser.getLogin());
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();//s
        }
        LOG.info("Grafana User Delete" + grafanaUser.toString());
        */

        solarSystemRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserDTO newUser() {
        UserRegisterDTO userRegisterDTO = new UserRegisterDTO("testRegister", "testtest");
        HttpEntity httpEntity = new HttpEntity(userRegisterDTO);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, UserDTO.class);
        return result.getBody();
    }

    private SolarSystemDTO addNewSolarSystem(UserDTO userDTO) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "jwt=" + userDTO.getJwt());
        RegisterSolarSystemDTO registerSolarSystemDTO = RegisterSolarSystemDTO.builder().name("testSystem").type(SolarSystemType.SELFMADE).maxSolarVoltage(60).build();
        HttpEntity httpEntity = new HttpEntity(registerSolarSystemDTO, headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);
        return responseSystem.getBody();

    }

    @ParameterizedTest
    @EnumSource(SolarSystemType.class)
    public void newSolar_RegisterSolarSystemDTO_OK(SolarSystemType type) {
        UserDTO userDTO = newUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "jwt=" + userDTO.getJwt());
        RegisterSolarSystemDTO registerSolarSystemDTO = RegisterSolarSystemDTO.builder().name("testSystem " + type).type(type).maxSolarVoltage(60).build();
        HttpEntity httpEntity = new HttpEntity(registerSolarSystemDTO, headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);
        assertThat(solarSystemRepository.existsById(responseSystem.getBody().getId())).isTrue();
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getSolar_systemID_OK() {
        UserDTO userDTO = newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem(userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "jwt=" + userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/" + (solarSystemDTO.getId()), HttpMethod.GET, httpEntity, SolarSystemDTO.class);
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSystem.getBody().getName()).isEqualTo(solarSystemDTO.getName());
        assertThat(responseSystem.getBody().getCreationDate()).isEqualTo(solarSystemDTO.getCreationDate());
        assertThat(responseSystem.getBody().getType()).isEqualTo(solarSystemDTO.getType());
    }

    @Test
    public void getSystems_OK() {
        UserDTO userDTO = newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem(userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "jwt=" + userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<SolarSystemDTO[]> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/all", HttpMethod.GET, httpEntity, SolarSystemDTO[].class);
        assertThat(responseSystem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSystem.getBody()[0].getName()).isEqualTo(solarSystemDTO.getName());
        assertThat(responseSystem.getBody()[0].getBuildingDate()).isEqualTo(solarSystemDTO.getBuildingDate());
        assertThat(responseSystem.getBody()[0].getCreationDate()).isNotNull();
        assertThat(responseSystem.getBody()[0].getType()).isEqualTo(solarSystemDTO.getType());
    }

    @Test
    public void deleteSystem_systemId_OK() {
        UserDTO userDTO = newUser();
        SolarSystemDTO solarSystemDTO = addNewSolarSystem(userDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Cookie", "jwt=" + userDTO.getJwt());
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system/" + solarSystemDTO.getId(), HttpMethod.POST, httpEntity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(solarSystemRepository.existsByIdAndIsDeleted(solarSystemDTO.getId())).isTrue();
        System.out.println(response.getBody());
    }
}
