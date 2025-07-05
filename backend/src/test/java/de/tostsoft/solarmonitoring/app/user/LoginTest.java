package de.tostsoft.solarmonitoring.app.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.app.service.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoginTest  extends AppBaseTest {

    @BeforeEach
    public void prepare() throws NoSuchFieldException, IllegalAccessException {
        clearDatabase();
    }

    public void setupSomeUsers(){
        addUser(true,"Test1");
        addUser(true,"Test2");
        addUser(true,"Test3");
    }

    @Autowired
    private UserService userService;

    @Test
    public void checkUserNameLoginUpperCase() throws JsonProcessingException {

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("TEST1")
                .password("password")
                .build();
        var ret = doRestRequest("/api/user/login", dto);
        Assertions.assertThat(ret.getStatusCode()).isEqualTo(HttpStatus.OK);

        var obj = objectMapper.readValue(ret.getBody(), UserDTO.class);

        Assertions.assertThat(obj.getName()).isEqualTo("TEST1");
    }

    @Test
    public void checkUserNameLoginLowercase() throws JsonProcessingException {

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("test1")
                .password("password")
                .build();

        var ret = doRestRequest("/api/user/login", dto);

        var obj = objectMapper.readValue(ret.getBody(),UserDTO.class);

        Assertions.assertThat(obj.getName()).isEqualTo("TEST1");
    }
    @Test
    public void checkMailLoginUpperCase() throws JsonProcessingException {

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("TEST1@local.host")
                .password("password")
                .build();
        var ret = doRestRequest("/api/user/login", dto);
        Assertions.assertThat(ret.getStatusCode()).isEqualTo(HttpStatus.OK);

        var obj = objectMapper.readValue(ret.getBody(), UserDTO.class);

        Assertions.assertThat(obj.getName()).isEqualTo("TEST1");
    }

    @Test
    public void checkMailLoginLowercase() throws JsonProcessingException {

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("test1@local.host")
                .password("password")
                .build();

        var ret = doRestRequest("/api/user/login", dto);

        var obj = objectMapper.readValue(ret.getBody(),UserDTO.class);

        Assertions.assertThat(obj.getName()).isEqualTo("TEST1");
    }

    @Test
    public void checkUsernameWrongPassword(){

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("test1")
                .password("NO_CORRECT_PASSWORD")
                .build();

        var ex = assertThrows(HttpClientErrorException.class,()->doRestRequest("/api/user/login", dto));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("invalid credentials");
    }

    @Test
    public void checkMailWrongPassword(){

        setupSomeUsers();

        var dto = UserLoginDTO.builder()
                .name("test1@local.host")
                .password("NO_CORRECT_PASSWORD")
                .build();

        var ex = assertThrows(HttpClientErrorException.class,()->doRestRequest("/api/user/login", dto));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("invalid credentials");
    }

    @Test
    public void checkUserNotFoundByUsername(){
        var dto = UserLoginDTO.builder()
                .name("test1")
                .password("NO_CORRECT_PASSWORD")
                .build();

        var ex = assertThrows(HttpClientErrorException.class,()->doRestRequest("/api/user/login", dto));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("invalid credentials");
    }

    @Test
    public void checkUserNotFoundByMail(){
        var dto = UserLoginDTO.builder()
                .name("test1@local.host")
                .password("NO_CORRECT_PASSWORD")
                .build();

        var ex = assertThrows(HttpClientErrorException.class,()->doRestRequest("/api/user/login", dto));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("invalid credentials");
    }



}
