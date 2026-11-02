package de.tostsoft.solarmonitoring.updater.mail;

import de.tostsoft.solarmonitoring.testlib.Waiter;
import de.tostsoft.solarmonitoring.updater.UpdaterBaseTest;
import de.tostsoft.solarmonitoring.updater.service.CleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@TestPropertySource(properties = {"monitoring.mail=testuser@local.host"})
public class CleanupFailureMailTest extends UpdaterBaseTest {

    @Autowired
    private CleanupService cleanupService;

    @BeforeEach
    public void setup() throws InterruptedException {
        clearDatabase();
    }

    private boolean hasAlertMail() {
        try {
            return mailhogTestService.fetchMails().getMailList().stream()
                    .anyMatch(m -> m.getSubject() != null && m.getSubject().contains("cleanup job failed"));
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void failedCleanupStepSendsAlertMail() throws Exception {

        var spyService = spy(cleanupService);
        doThrow(new RuntimeException("test failure message")).when(spyService).resetDailyRegistrations();

        spyService.runDailyCleanup();

        Waiter.waitToHappen(this::hasAlertMail, 10_000);

        var mail = mailhogTestService.fetchMails().getMailList().stream()
                .filter(m -> m.getSubject() != null && m.getSubject().contains("cleanup job failed"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Alert mail not found"));

        assertThat(mail.getTo()).contains("testuser@local.host");
        assertThat(mail.getSubject()).isEqualTo("ALERT: Nightly cleanup job failed - http://localhost:8051");
        assertThat(mail.getContent()).contains("on http://localhost:8051");
        assertThat(mail.getContent()).contains("resetDailyRegistrations");
        assertThat(mail.getContent()).contains("test failure message");
        assertThat(mail.getContent()).doesNotContain("at de.tostsoft");
        assertThat(mail.getContent()).doesNotContain("at java.base");
    }

    @Test
    public void successfulCleanupSendsNoAlertMail() throws Exception {

        cleanupService.runDailyCleanup();

        //mail sending is async, give it time to arrive
        Thread.sleep(3000);

        assertThat(hasAlertMail()).isFalse();
    }
}
