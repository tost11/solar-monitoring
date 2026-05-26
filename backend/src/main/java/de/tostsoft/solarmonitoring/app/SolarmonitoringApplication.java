package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.service.MailService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringApplication {

    @Value("${monitoring.mail:#{null}}")
    private String monitoringMail;

    @Autowired
    private MailService mailService;

    private Logger LOG = LoggerFactory.getLogger(SolarmonitoringApplication.class);

    @PostConstruct
    private void checkMailSendingWorking(){
        if(monitoringMail == null){
            LOG.warn("Start mail could not be sent because no monitoring mail is set");
        }else{
            mailService.sendMail(monitoringMail,"Solar-Monitoring Application start up mail","The Main Application was started and mail service is working");
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringApplication.class, args);
    }
}
