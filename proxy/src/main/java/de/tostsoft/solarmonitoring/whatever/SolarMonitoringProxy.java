package de.tostsoft.solarmonitoring.whatever;

import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "de.tostsoft.solarmonitoring",exclude = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(basePackages = {"de.tostsoft.solarmonitoring"}, excludeFilters={
    @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, value=InfluxConnection.class),
    @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, value= InfluxTaskService.class)})
public class SolarMonitoringProxy {

    public static void main(String[] args) {
        SpringApplication.run(SolarMonitoringProxy.class, args);
    }
}
