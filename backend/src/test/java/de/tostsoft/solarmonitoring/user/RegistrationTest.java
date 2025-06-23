package de.tostsoft.solarmonitoring.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.dtos.users.RegisterInfoDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
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
    private CaptchaRepository captchaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailhogTestService mailhogTestService;

    @BeforeEach
    public void runBefore() {
        mailhogTestService.deleteAllMessages();
    }

    private Logger LOG = LoggerFactory.getLogger(RegistrationTest.class);

    //TODO some more test: activation url invlid url(id), check if sign in without activation is possible, validation for paramters

    @Test
    public void registerUserSuccessFul() throws JsonProcessingException, InterruptedException {
        var res = doRestRequest("api/user/register");

        var captcha = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var capt = captchaRepository.getCaptchaByBase64Image(captcha.getCaptcha());

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setCaptchaText(capt.getText());
        dto.setCaptcha(capt.getBase64Image());
        dto.setName("Test");
        dto.setPassword("abcTest123!");
        dto.setMail("test@local.host");

        doRestRequest("api/user/register",dto,HttpMethod.POST);

        var user = userRepository.findByName("test");//lower case becase so saved in database for matching
        assertThat(user.getViewName()).isEqualTo("Test");
        assertThat(user.getName()).isEqualTo("test");
        assertThat(user.isActivated()).isEqualTo(false);

        //check if password was encoded correctly
        assertThat(passwordEncoder.matches("abcTest123!",user.getPassword())).isTrue();

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

        user = userRepository.findByName("test");//lower case becase so saved in database for matching
        assertThat(user.isActivated()).isEqualTo(true);
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
