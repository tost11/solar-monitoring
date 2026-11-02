package de.tostsoft.solarmonitoring.updater.mail;

import de.tostsoft.solarmonitoring.updater.UpdaterBaseTest;
import de.tostsoft.solarmonitoring.updater.service.CleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "monitoring.cleanupFailureAlert.enabled=false"})
public class CleanupFailureMailDisabledTest extends UpdaterBaseTest {

    @Autowired
    private CleanupService cleanupService;

    @BeforeEach
    public void setup() throws InterruptedException {
        clearDatabase();
    }

    @Test
    public void failedCleanupStepSendsNoAlertMailWhenDisabled() throws Exception {

        var spyService = spy(cleanupService);
        doThrow(new RuntimeException("test failure message")).when(spyService).resetDailyRegistrations();

        spyService.runDailyCleanup();

        //mail sending is async, give it time to arrive
        Thread.sleep(3000);

        boolean hasAlertMail = mailhogTestService.fetchMails().getMailList().stream()
                .anyMatch(m -> m.getSubject() != null && m.getSubject().contains("cleanup job failed"));

        assertThat(hasAlertMail).isFalse();
    }
}
