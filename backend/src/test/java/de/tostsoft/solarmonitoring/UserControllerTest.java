package de.tostsoft.solarmonitoring;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.neo4j.AutoConfigureDataNeo4j;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;



@SpringBootTest(classes = {SolarmonitoringApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Value("${influx.token}")
    private String token;
    @Value("${influx.url}")
    private String url;
    @Value("&{influx.organisation}")
    private String influxOrganisation;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private InfluxDBClient influxDBClient;

    public InfluxDBClient getClient() {
        return influxDBClient;
    }

    @LocalServerPort
    int randomServerPort;

    @BeforeEach
    private void init(){
        influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), influxOrganisation);
        User user = new User("jonas", "12345678",0L,"");
        user.setPassword(passwordEncoder.encode(user.getPassword()));


    }



    @Test
    public void login_ExistUser_OK() {
        UserLoginDTO user = new UserLoginDTO("debug", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity, UserDTO.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo(user.getName());



    }
    @Test
    public void register_newUser_OK() {
        UserRegisterDTO user = new UserRegisterDTO("debug", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, UserDTO.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo(user.getName());



    }
}
