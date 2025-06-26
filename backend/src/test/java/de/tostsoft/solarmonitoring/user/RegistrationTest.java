package de.tostsoft.solarmonitoring.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.dtos.users.RegisterInfoDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegistrationTest  extends ApplicationBaseRestTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Logger LOG = LoggerFactory.getLogger(RegistrationTest.class);

    UserRegisterDTO createValidUserDTU() throws JsonProcessingException {
        var res = doRestRequest("api/user/register");
        var captcha = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var capt = captchaRepository.getCaptchaByBase64Image(captcha.getCaptcha());
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText(capt.getText());
        dto.setCaptcha(capt.getBase64Image());
        dto.setName("Test");
        dto.setPassword("abcTest123!");
        dto.setMail("test@local.host");

        return dto;
    }

    @Test
    public void registerUserSuccessFul() throws JsonProcessingException, InterruptedException {
        var res = doRestRequest("api/user/register");

        var captcha = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var capt = captchaRepository.getCaptchaByBase64Image(captcha.getCaptcha());

        String password = "abcTest123!";
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText(capt.getText());
        dto.setCaptcha(capt.getBase64Image());
        dto.setName("Test");
        dto.setPassword(password);
        dto.setMail("test@local.host");

        doRestRequest("api/user/register",dto,HttpMethod.POST);

        var registerUser = registerUserRepository.findByName("test");//lower case becase so saved in database for matching
        assertThat(registerUser.getViewName()).isEqualTo("Test");
        assertThat(registerUser.getName()).isEqualTo("test");

        //check if password was encoded correctly
        assertThat(passwordEncoder.matches(password,registerUser.getPassword())).isTrue();

        //solved captcha removed from database
        assertThat(captchaRepository.getCaptchaByBase64Image(capt.getBase64Image())).isNull();

        var mailHogResponse = mailhogTestService.fetchMails();

        assertThat(mailHogResponse.getSize()).isEqualTo(1);
        assertThat(mailHogResponse.getMailList()).hasSize(1);

        var mail =  mailHogResponse.getMailList().get(0);
        assertThat(mail.getSubject()).isEqualTo("Solar Monitoring Activation");

        // Regular Expression to extract URL from the string
        String regexStr = "\\b((?:https?|ftp|file):"
        + "\\/\\/[a-zA-Z0-9+&@#\\/%?=~_|!:,.;]*"
        + "[a-zA-Z0-9+&@#\\/%=~_|])";

        // Compile the Regular Expression pattern
        Pattern pattern = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);

        // Create a Matcher that matches the pattern with the input string
        Matcher matcher = pattern.matcher(mail.getContent());
        assertThat(matcher.find()).isTrue();
        String url = matcher.group();

        LOG.info("Extracted mail link is: "+url);
        url = StringUtils.replace(url,"8050",""+getServerPort(),1);
        LOG.info("Replaced port mail link is: "+url);

        //activate account
        RestTemplate restTemplate = new RestTemplate();
        res = restTemplate.exchange(url,HttpMethod.GET,null,String.class);

        LOG.info("Activation response is: "+res.getBody());

        var user = userRepository.findByName("test");//lower case becase so saved in database for matching
        assertThat(user.getViewName()).isEqualTo("Test");
        assertThat(user.getName()).isEqualTo("test");

        //check if password was encoded correctly
        assertThat(passwordEncoder.matches(password,user.getPassword())).isTrue();

        //solved captcha removed from database
        assertThat(captchaRepository.getCaptchaByBase64Image(capt.getBase64Image())).isNull();

        //check bucket exists
        assertThat(influxConnection.getBuckets().stream().filter(b->StringUtils.equals(b.getName(),user.getInfluxBucketName())).count()).isEqualTo(1);
    }

    @Test
    public void testCaptchaNotInDatabase(){
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText("1234");
        dto.setCaptcha("whatever");
        dto.setName("Test");
        dto.setPassword("abcTest123!");
        dto.setMail("tost@local.host");

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
        dto.setMail("tost@local.host");

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("Captcha not answered correct");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"","NOT_A_MAIL","a@b.c","@local.host","test@local","test@local.","test@.local","test@loc@al.host","test@loc&al.host",
    "abjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghijabjdefghij@local.host"})
    public void checkInvalidMail(String mail) throws JsonProcessingException {
        var dto = createValidUserDTU();

        dto.setMail(mail);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("mail");
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"","a","aa","aaa","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","a a"})
    public void checkInvalidUsername(String name) throws JsonProcessingException {
        var dto = createValidUserDTU();

        dto.setName(name);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aaaa","äüöÄÜÖé_-0123456789","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    public void checkValidUsername(String name) throws JsonProcessingException {
        var dto = createValidUserDTU();

        dto.setName(name);

        doRestRequest("api/user/register",dto,HttpMethod.POST);

        var registerUser = registerUserRepository.findByName(StringUtils.toRootLowerCase(name));//lower case because so saved in database for matching

        assertThat(registerUser.getViewName()).isEqualTo(name);
        assertThat(registerUser.getName()).isEqualTo(StringUtils.toRootLowerCase(name));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"","abcTest123","abcTest!","ABCTEST123!","abctest123!","aA1!",})
    public void checkInvalidPassword(String password) throws JsonProcessingException {
        var dto = createValidUserDTU();

        dto.setPassword(password);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/user/register",dto, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcTest123!","ABCTEST123!ß","ABCTEST123!ä","ABCTEST123!ö","ABCTEST123!ü","abctest123!Ü","abctest123!Ö","abctest123!Ä"})
    public void checkValidPassword(String password) throws JsonProcessingException {
        var dto = createValidUserDTU();

        dto.setPassword(password);

        doRestRequest("api/user/register",dto,HttpMethod.POST);

        var registerUser = registerUserRepository.findByName(StringUtils.toRootLowerCase(dto.getName()));//lower case because so saved in database for matching

        assertThat(passwordEncoder.matches(password,registerUser.getPassword())).isTrue();
    }

    @Test
    public void checkLoginWithoutActivationNotPossible() throws JsonProcessingException {
        var dto = createValidUserDTU();

        doRestRequest("api/user/register",dto,HttpMethod.POST);

        var ex = assertThrows(HttpClientErrorException.class,()->signIn(dto.getName(),dto.getPassword()));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("invalid credentials");
    }

    @Test
    public void checkInvalidActivationUrl() throws JsonProcessingException {
        String url = "http://localhost:"+getServerPort()+"/api/user/activate/NOT_A_VALID_URL";

        var restTemplate = new RestTemplate();

        var ex = assertThrows(HttpClientErrorException.class,()->restTemplate.exchange(url,HttpMethod.GET,null,String.class));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).containsIgnoringCase("Invalid activation link");
    }
}
