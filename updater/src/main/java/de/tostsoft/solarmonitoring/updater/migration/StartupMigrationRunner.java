package de.tostsoft.solarmonitoring.updater.migration;

import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class StartupMigrationRunner {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running startup migrations...");
        migrateTokensToList();
        log.info("Startup migrations completed.");
    }

    /**
     * Migrates existing single-token systems to the new tokens list structure.
     * For each system that has a legacy `token` field but no entries in the `tokens` list,
     * creates an AccessToken entry with the existing bcrypt hash.
     * Idempotent: skips systems that already have tokens in the list.
     */
    private void migrateTokensToList() {
        log.info("Starting token migration: converting legacy token field to tokens list...");

        int migratedCount = 0;
        int skippedCount = 0;

        Pageable pageable = PageRequest.of(0, 100);
        Page<SolarSystem> page = solarSystemRepository.findAll(pageable);

        while (true) {
            for (SolarSystem system : page) {
                // Skip if tokens list already has entries (already migrated)
                if (!CollectionUtils.isEmpty(system.getTokens())) {
                    skippedCount++;
                    continue;
                }

                // Skip if no legacy token to migrate
                if (system.getToken() == null || system.getToken().isEmpty()) {
                    skippedCount++;
                    continue;
                }

                // Create a new AccessToken from the legacy bcrypt hash
                var accessToken = AccessToken.builder()
                        .id(UUID.randomUUID().toString())
                        .name("Default (migrated)")
                        .hash(system.getToken())
                        .purpose(TokenPurpose.DATA_PUSH_REST)
                        .createdAt(system.getCreationDate() != null ? system.getCreationDate() : LocalDateTime.now())
                        .expiresAt(null)
                        .build();

                List<AccessToken> tokens = new ArrayList<>();
                tokens.add(accessToken);
                solarSystemRepository.updateTokens(system.getId(), tokens);
                migratedCount++;
            }

            if (!page.hasNext()) {
                break;
            }
            page = solarSystemRepository.findAll(page.nextPageable());
        }

        log.info("Token migration completed. Migrated: {}, Skipped (already migrated or no token): {}", migratedCount, skippedCount);
    }
}
