package de.tostsoft.solarmonitoring.updater.migration;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class StartupMigrationRunner {

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        //log.info("Running startup migrations...");
        //TODO add migration here
        //log.info("Startup migrations completed.");
    }
}
