package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
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

    @BeforeEach
    public void setup() {
        clearDatabase();
    }

    @Test
    public void checkValidCaptchasNotCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        captchaRepository.save(capt);

        capt.setCreatedAt(Instant.now().minus(55, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(1);
    }

    @Test
    public void checkOlCapchasCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE");
        capt.setText("TEST_TEXT");

        capt.setCreatedAt(Instant.now().minus(65, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(0);
    }
}
