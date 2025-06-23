package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.service.MailService;
import de.tostsoft.solarmonitoring.updater.service.InfluxTaskScheduler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringUpdater{

    @Autowired
    private MailService mailService;

    @PostConstruct
    private void checkMailSendingWorking(){
        mailService.sendMail("tost@tost-soft.de","Updater start up mail","The Updater Application was started and mail service is working");
    }

    private static final Logger LOG = LoggerFactory.getLogger(SolarmonitoringUpdater.class);

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringUpdater.class, args);
    }
}
