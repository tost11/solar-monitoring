package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.dtos.ApiErrorResponseDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.neo4j.AutoConfigureDataNeo4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;




@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataNeo4j
@SpringBootTest(classes = {SolarmonitoringApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(UserControllerTest.class);
    @Autowired
    private UserRepository userRepository;

    @Value("${grafana.user}")
    private String grafanaUser;

    @Value("${grafana.password}")
    private String grafanaPassword;

    @Value("${proxy.grafana.target.url}")
    private String grafanaUrl;

    @Value("${influx.token}")
    private String influxToken;
    @Value("${influx.url}")
    private String influxUrl;
    @Value("&{influx.organisation}")
    private String influxOrganisation;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private AuthenticationManager authenticationManager;

    @LocalServerPort
    int randomServerPort;

    @BeforeEach
    public void init(){
        cleanUpData();
    }


    private void cleanUpData(){
        //TODO fix that
        /*
            LOG.info("Delete Influx bucket");
            influxConnection.deleteBucket(grafanaUser.getLogin());
         */

        userRepository.deleteAll();
        UserRegisterDTO user = new UserRegisterDTO("testLogin", "testtest");
        userService.registerUser(user);
    }



    @Test
    public void login_ExistUser_OK() {
        UserLoginDTO user = new UserLoginDTO("testLogin", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity, UserDTO.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo(user.getName());
    }
    @Test
    public void login_NotExistUser_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("newUser", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);

        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity,String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            assertThat(response.getError()).isEqualTo("invalid credentials");
        }
    }
    @Test
    public void login_EmptyName_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);

        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity,String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            assertThat(response.getError()).isEqualTo("name is empty");
        }
    }
    @Test
    public void login_EmptyPassword_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("testLogin", "");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);

        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity,String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            assertThat(response.getError()).isEqualTo("password is empty");
        }
    }
    @Test
    public void register_NewUser_OK() {
        UserRegisterDTO newUser = new UserRegisterDTO("testRegister", "123456789");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(newUser);
        ResponseEntity<UserDTO> result = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, UserDTO.class);
        User databaseUser=userRepository.findByNameIgnoreCase("testRegister");
       Authentication authentication = null;
        try{
             authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(newUser.getName(), "123456789"));
        }catch (BadCredentialsException e){
            LOG.error("Bad Credentials to Authenticate");
           authentication.setAuthenticated(false);
        }
        assertThat(authentication.isAuthenticated()).isTrue();//Authentication is true
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo(newUser.getName());
        assertThat(newUser.getName()).isEqualTo(databaseUser.getName());

    }
    @Test
    public void register_ExistUser_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("testLogin", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            System.out.println(response.getError());
            assertThat(response.getError()).isEqualTo("\n Username is already taken");
        }



    }
    @Test
    public void register_UserNameToShort_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("t", "testtest");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            System.out.println(response.getError());
            assertThat(response.getError()).isEqualTo("\n Username must contain at least 4 characters");
        }
    }
    @Test
    public void register_EmptyPassword_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("test", "");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            System.out.println(response.getError());
            assertThat(response.getError()).isEqualTo("\n No password has been entered");
        }
    }
    @Test
    public void register_PasswordToShort_BadRequest() throws JsonProcessingException {
        UserLoginDTO user = new UserLoginDTO("test", "1");
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity httpEntity = new HttpEntity(user);
        try {
            restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/register", HttpMethod.POST, httpEntity, String.class);
        }catch (HttpClientErrorException e){
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ObjectMapper objectMapper= new ObjectMapper();
            var response=objectMapper.readValue(e.getResponseBodyAsString(),ApiErrorResponseDTO.class);
            System.out.println(response.getError());
            assertThat(response.getError()).isEqualTo("\n Password must contain at least 8 characters");
        }
    }

}
