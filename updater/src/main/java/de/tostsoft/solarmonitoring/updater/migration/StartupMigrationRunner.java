package de.tostsoft.solarmonitoring.updater.migration;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class StartupMigrationRunner {

    private final SystemInformationsMigration systemInformationsMigration;

    public StartupMigrationRunner(SystemInformationsMigration systemInformationsMigration) {
        this.systemInformationsMigration = systemInformationsMigration;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running startup migrations...");
        systemInformationsMigration.migrateAllSystems();
        log.info("Startup migrations completed.");
    }
}
