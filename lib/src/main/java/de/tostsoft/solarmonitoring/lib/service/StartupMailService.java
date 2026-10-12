package de.tostsoft.solarmonitoring.lib.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class StartupMailService {

    private static final Logger LOG = LoggerFactory.getLogger(StartupMailService.class);

    @Value("${monitoring.mail:#{null}}")
    private String monitoringMail;

    @Value("${spring.application.name:unknown}")
    private String appName;

    @Value("${fulldomain:}")
    private String fulldomain;

    @Autowired
    private MailService mailService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (monitoringMail == null) {
            LOG.warn("Startup mail skipped: monitoring.mail not set");
            return;
        }

        String domain = StringUtils.isNotEmpty(fulldomain) ? " on " + fulldomain : "";
        String subject = "Solar-Monitoring " + appName + " started"
                + (StringUtils.isNotEmpty(fulldomain) ? " - " + fulldomain : "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String body = "The " + appName + " started at " + timestamp + domain + ". Mail service is working.";

        mailService.sendMail(monitoringMail, subject, body);
    }
}
