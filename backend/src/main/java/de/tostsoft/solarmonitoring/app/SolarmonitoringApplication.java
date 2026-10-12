package de.tostsoft.solarmonitoring.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring")
public class SolarmonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringApplication.class, args);
    }
}
