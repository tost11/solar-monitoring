package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.updater.service.InfluxTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringUpdater{

    private static final Logger LOG = LoggerFactory.getLogger(SolarmonitoringUpdater.class);

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringUpdater.class, args);
    }
}
