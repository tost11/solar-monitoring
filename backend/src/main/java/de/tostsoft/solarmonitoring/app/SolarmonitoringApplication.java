package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.service.MailService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringApplication {

    @Autowired
    private MailService mailService;

    @PostConstruct
    private void checkMailSendingWorking(){
        mailService.sendMail("tost@tost-soft.de","Application start up mail","The Main Application was started and mail service is working");
    }


    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringApplication.class, args);
    }
}
