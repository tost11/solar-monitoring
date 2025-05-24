package de.tostsoft.solarmonitoring.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.dtos.users.RegisterInfoDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegistrationTest  extends ApplicationBaseRestTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Autowired
    private CaptchaRepository captchaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void registerUserSuccessFul() throws JsonProcessingException {
        var res = doRestRequest("api/user/register");

        var captcha = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var capt = captchaRepository.getCaptchaByBase64Image(captcha.getCaptcha());

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText(capt.getText());
        dto.setCaptcha(capt.getBase64Image());
        dto.setName("Test");
        dto.setPassword("abcTest123!");

        var resRegister = doRestRequest("api/user/register",dto,HttpMethod.POST);
        var userDTO = objectMapper.readValue(resRegister.getBody(), UserDTO.class);

        //check jwt working
        doRestRequest("api/user","",HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+userDTO.getJwt()));

        var user = userRepository.findByName("test");//lower case becase so saved in database for matching
        assertThat(user.getViewName()).isEqualTo("Test");
        assertThat(user.getName()).isEqualTo("test");

        //check if password was encoded correctly
        assertThat(passwordEncoder.matches("abcTest123!",user.getPassword())).isTrue();

        //solved captcha removed from database
        assertThat(captchaRepository.getCaptchaByBase64Image(capt.getBase64Image())).isNull();
    }

    @Test
    public void testCaptchaNotInDatabase(){
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText("1234");
        dto.setCaptcha("whatever");
        dto.setName("Test");
        dto.setPassword("abcTest123!");


        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("Captcha unknown (please reload image)");
    }

    @Test
    public void testCaptchaIncorrectInDatabase() throws JsonProcessingException {
        var res = doRestRequest("api/user/register");

        var captcha = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var capt = captchaRepository.getCaptchaByBase64Image(captcha.getCaptcha());

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText("1234");
        dto.setCaptcha(capt.getBase64Image());
        dto.setName("Test");
        dto.setPassword("abcTest123!");

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("Captcha not answered correct");
    }
}
