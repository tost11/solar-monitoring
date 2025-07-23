package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.service.MailService;
import de.tostsoft.solarmonitoring.updater.service.InfluxTaskScheduler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringUpdater{

    @Value("${monitoring.mail:#{null}}")
    private String monitoringMail;

    @Autowired
    private MailService mailService;

    private static final Logger LOG = LoggerFactory.getLogger(SolarmonitoringUpdater.class);

    @PostConstruct
    private void checkMailSendingWorking(){
        if(monitoringMail == null){
            LOG.warn("Start mail could not be sent because no monitoring mail is set");
        }else{
            mailService.sendMail(monitoringMail,"Solar-Monitoring Updater start up mail","The Updater Application was started and mail service is working");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringUpdater.class, args);
    }
}
