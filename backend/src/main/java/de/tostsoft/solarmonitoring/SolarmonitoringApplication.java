package de.tostsoft.solarmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
public class SolarmonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolarmonitoringApplication.class, args);
    }
}
