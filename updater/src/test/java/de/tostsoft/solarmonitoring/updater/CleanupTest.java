package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.model.RegisterUser;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.RegisterUserRepository;
import de.tostsoft.solarmonitoring.updater.service.CleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanupTest extends UpdaterBaseTest {

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private CaptchaRepository captchaRepository;

    @Autowired
    private RegisterUserRepository registerUserRepository;

    @BeforeEach
    public void setup() {
        clearDatabase();
    }

    @Test
    public void checkValidCaptchasNotCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE1");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        captchaRepository.save(capt);

        capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE2");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        capt.setCreatedAt(Instant.now().minus(55, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE3");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        capt.setCreatedAt(Instant.now().minus(65, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(2);
    }

    @Test
    public void checkOldCaptchasCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE");
        capt.setText("TEST_TEXT");

        capt.setCreatedAt(Instant.now().minus(65, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(0);
        }

    @Test
    public void checkValidRegisterUsersNotCleanedUp(){

        var user = RegisterUser.builder()
                        .mail("test@local.host")
                        .password("abcTest123!")
                        .name("test")
                        .viewName("Test")
                        .createdAt(Instant.now().toEpochMilli())
                        .build();

        registerUserRepository.save(user);

        user = RegisterUser.builder()
                .mail("test2@local.host")
                .password("abcTest123!")
                .name("test2")
                .viewName("Test2")
                .createdAt(Instant.now().toEpochMilli())
                .build();

        user.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(55,ChronoUnit.MINUTES).toEpochMilli());
        registerUserRepository.save(user);


        user = RegisterUser.builder()
                .mail("test3@local.host")
                .password("abcTest123!")
                .name("test3")
                .viewName("Test3")
                .createdAt(Instant.now().toEpochMilli())
                .build();

        user.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(65,ChronoUnit.MINUTES).toEpochMilli());
        registerUserRepository.save(user);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(registerUserRepository.count()).isEqualTo(2);
    }

    @Test
    public void checkOldRegisterUsersCleanedUp(){

        var user = RegisterUser.builder()
                .mail("test@local.host")
                .password("abcTest123!")
                .name("test")
                .viewName("Test")
                .createdAt(Instant.now().minus(24, ChronoUnit.HOURS).minus(5,ChronoUnit.MINUTES).toEpochMilli())
                .build();

        registerUserRepository.save(user);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(registerUserRepository.count()).isEqualTo(0);
    }


}
