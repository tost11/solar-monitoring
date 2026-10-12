package de.tostsoft.solarmonitoring.testlib.startup;

import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseStartupMailNotSentTest {

    @Autowired
    protected MailhogTestService mailhogTestService;

    @Test
    void noStartupMailWhenNotConfigured() throws Exception {
        Thread.sleep(3000);

        var mails = mailhogTestService.fetchMails();
        assertThat(mails.getMailList()).noneMatch(m -> m.getSubject() != null && m.getSubject().contains("started"));
    }
}
