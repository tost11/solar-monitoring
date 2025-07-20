package de.tostsoft.solarmonitoring.testlib.BaseTests;

import com.influxdb.client.service.UsersService;
import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.model.RegisterUser;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.RegisterUserRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BaseRepositoryTest {

    private final UserRepository userRepository;
    private final RegisterUserRepository registerUserRepository;
    private final CaptchaRepository captchaRepository;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        registerUserRepository.deleteAll();
        captchaRepository.deleteAll();
    }

    public BaseRepositoryTest(ApplicationContext context) {
        userRepository = context.getBean(UserRepository.class);
        registerUserRepository = context.getBean(RegisterUserRepository.class);
        captchaRepository = context.getBean(CaptchaRepository.class);
    }

    User createValidUser(){
        return User.builder()
                .password("abcTest123!")
                .name("test")
                .viewName("Test")
                .mail("test@local.host")
                .influxBucketName("whatever")
                .creationDate(LocalDateTime.now(ZoneId.of("UTC")))
                .isAdmin(false)
                .build();
    }

    RegisterUser createValidRegisterUser(){
        return RegisterUser.builder()
                .password("abcTest123!")
                .name("test")
                .viewName("Test")
                .mail("test@local.host")
                .createdAt(Instant.now().toEpochMilli())
                .build();
    }

    Captcha createValidCaptcha(){
        return Captcha.builder()
                .text("NO_VALID_TEXT")
                .base64Image("NOT_VALID_IMAGE")
                .build();
    }

    @Test
    void checkUserRepositoryUniqueName(){
        var user = createValidUser();
        userRepository.save(user);
        var user2 = createValidUser();
        user2.setMail("test2@local.host");
        user2.setInfluxBucketName("whatever2");

        var ex = assertThrows(DuplicateKeyException.class,()-> userRepository.save(user2));
        assertThat(ex.getMessage()).containsIgnoringCase("name dup key: { name: \"test\" }");
    }

    @Test
    void checkUserRepositoryUniqueMail(){
        var user = createValidUser();
        userRepository.save(user);
        var user2 = createValidUser();
        user2.setName("test2");
        user2.setInfluxBucketName("whatever2");

        var ex = assertThrows(DuplicateKeyException.class,()-> userRepository.save(user2));
        assertThat(ex.getMessage()).containsIgnoringCase("mail dup key: { mail: \"test@local.host\" }");
    }

    @Test
    void checkUserRepositoryUniqueInfluxBucketName(){
        var user = createValidUser();
        userRepository.save(user);
        var user2 = createValidUser();
        user2.setName("test2");
        user2.setMail("test@local.host2");

        var ex = assertThrows(DuplicateKeyException.class,()-> userRepository.save(user2));
        assertThat(ex.getMessage()).containsIgnoringCase("influxBucketName dup key: { influxBucketName: \"whatever\" }");
    }


    @Test
    void checkRegisterUserRepositoryUniqueName(){
        var user = createValidRegisterUser();
        registerUserRepository.save(user);
        var user2 = createValidRegisterUser();
        user2.setMail("test2@local.host");

        var ex = assertThrows(DuplicateKeyException.class,()-> registerUserRepository.save(user2));
        assertThat(ex.getMessage()).containsIgnoringCase("name dup key: { name: \"test\" }");
    }

    @Test
    void checkRegisterUserRepositoryUniqueMail(){
        var user = createValidRegisterUser();
        registerUserRepository.save(user);
        var user2 = createValidRegisterUser();
        user2.setName("test2");

        var ex = assertThrows(DuplicateKeyException.class,()-> registerUserRepository.save(user2));
        assertThat(ex.getMessage()).containsIgnoringCase("mail dup key: { mail: \"test@local.host\" }");
    }


    @Test
    void checkCaptchaRepositoryUniqueName(){
        var capt = createValidCaptcha();
        captchaRepository.save(capt);
        var capt2 = createValidCaptcha();

        var ex = assertThrows(DuplicateKeyException.class,()-> captchaRepository.save(capt2));
        assertThat(ex.getMessage()).containsIgnoringCase("base64Image dup key: { base64Image: \"NOT_VALID_IMAGE\" }");
    }

}
