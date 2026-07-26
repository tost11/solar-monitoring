package de.tostsoft.solarmonitoring.updater.migration;

import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupMigrationRunner {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running startup migrations...");
        log.info("Startup migrations completed.");
    }
}
