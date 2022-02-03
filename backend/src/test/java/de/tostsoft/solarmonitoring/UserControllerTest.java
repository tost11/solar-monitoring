package de.tostsoft.solarmonitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.User;
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
import org.springframework.boot.test.autoconfigure.data.neo4j.AutoConfigureDataNeo4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureDataNeo4j
@SpringBootTest(classes = {SolarmonitoringApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(UserControllerTest.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private Neo4jTemplate neo4jTemplate;

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
    private GrafanaService grafanaService;
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
        RestTemplate restTemplate = new RestTemplate();
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
            influxConnection.deleteBucket(grafanaUser.getLogin());
            LOG.info("Grafana User Delete"+grafanaUser.toString());
            grafanaService.deleteUser(grafanaUser.getId());



        }


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
