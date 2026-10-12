package de.tostsoft.solarmonitoring.updater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringUpdater{

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringUpdater.class, args);
    }
}
