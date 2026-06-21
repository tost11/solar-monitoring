package de.tostsoft.solarmonitoring.updater.migration;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.SystemInformations;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class SystemInformationsMigration {

    private final SolarSystemRepository solarSystemRepository;

    public SystemInformationsMigration(SolarSystemRepository solarSystemRepository) {
        this.solarSystemRepository = solarSystemRepository;
    }

    public void migrateAllSystems() {
        log.info("Starting SystemInformations migration...");

        List<SolarSystem> allSystems = solarSystemRepository.findAllWithDeleted();
        int migratedCount = 0;
        int skippedCount = 0;
        int nameFieldMigrated = 0;

        for (SolarSystem system : allSystems) {
            boolean needsSave = false;

            // Migrate systemInformations object if missing
            if (system.getSystemInformations() == null ||
                system.getSystemInformations().getViewName() == null) {

                SystemInformations info = SystemInformations.builder()
                    .name(system.getName())
                    .viewName(system.getViewName())
                    .publicName(null)
                    .description(null)
                    .maxInstalledSolarPower(system.getMaxInstalledSolarPower())
                    .maxInverterOutputPower(system.getMaxInverterOutputPower())
                    .batteryCapacity(null)
                    .buildingDate(system.getBuildingDate())
                    .electricityPrice(system.getElectricityPrice())
                    .electricityPriceFeedIn(system.getElectricityPriceFeedIn())
                    .build();

                system.setSystemInformations(info);
                migratedCount++;
                needsSave = true;

                log.debug("Migrated systemInformations for system: {} (ID: {})", system.getName(), system.getId());
            }
            // Migrate name field if missing in existing systemInformations
            else if (system.getSystemInformations().getName() == null) {
                system.getSystemInformations().setName(system.getName());
                nameFieldMigrated++;
                needsSave = true;

                log.debug("Migrated name field for system: {} (ID: {})", system.getName(), system.getId());
            } else {
                skippedCount++;
            }

            if (needsSave) {
                solarSystemRepository.save(system);
            }
        }

        log.info("SystemInformations migration completed. Full migration: {}, Name field only: {}, Skipped: {}, Total: {}",
                 migratedCount, nameFieldMigrated, skippedCount, allSystems.size());
    }
}
