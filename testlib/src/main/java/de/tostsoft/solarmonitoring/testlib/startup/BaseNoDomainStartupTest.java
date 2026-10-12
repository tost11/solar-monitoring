package de.tostsoft.solarmonitoring.testlib.startup;

import de.tostsoft.solarmonitoring.testlib.Waiter;
import de.tostsoft.solarmonitoring.testlib.model.Mail;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseNoDomainStartupTest {

    @Autowired
    protected MailhogTestService mailhogTestService;

    protected abstract String getAppName();

    @Test
    void startsWithoutDomain() throws Exception {
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

        assertThat(startupMail.getSubject()).isEqualTo("Solar-Monitoring " + getAppName() + " started");
        assertThat(startupMail.getContent()).contains("The " + getAppName() + " started at");
        assertThat(startupMail.getContent()).doesNotContain(" on ");
        assertThat(startupMail.getContent()).contains("Mail service is working.");
    }
}
