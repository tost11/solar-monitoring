package de.tostsoft.solarmonitoring.testlib.startup;

import de.tostsoft.solarmonitoring.testlib.Waiter;
import de.tostsoft.solarmonitoring.testlib.model.Mail;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseStartupMailSentTest {

    @Autowired
    protected MailhogTestService mailhogTestService;

    protected abstract String getAppName();

    protected abstract String getDomain();

    @Test
    void startupMailIsSent() throws Exception {
        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getMailList().stream()
                        .anyMatch(m -> m.getSubject() != null && m.getSubject().contains("started"));
            } catch (Exception e) {
                return false;
            }
        }, 10_000);

        var mails = mailhogTestService.fetchMails();
        Mail startupMail = mails.getMailList().stream()
                .filter(m -> m.getSubject() != null && m.getSubject().contains("started"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Startup mail not found"));

        String expectedSubject = "Solar-Monitoring " + getAppName() + " started - " + getDomain();
        assertThat(startupMail.getSubject()).isEqualTo(expectedSubject);
        assertThat(startupMail.getContent()).contains("The " + getAppName() + " started at");
        assertThat(startupMail.getContent()).contains(" on " + getDomain());
        assertThat(startupMail.getContent()).contains("Mail service is working.");
    }
}
