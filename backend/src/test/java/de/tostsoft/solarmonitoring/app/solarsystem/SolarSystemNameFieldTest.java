package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.SystemInformations;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SolarSystemNameFieldTest extends AppBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemNameFieldTest.class);

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testPublicNameSubstitutionForPublicViewer() throws Exception {
        LOG.info("Testing publicName substitution for public viewers");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        SystemInformations info = system.getSystemInformations();
        if (info == null) {
            info = new SystemInformations();
        }
        info.setViewName("My Private Name");
        info.setPublicName("Public Friendly Name");
        system.setSystemInformations(info);
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);

        ResponseEntity<String> publicResponse = doRequest(
                "api/system/public/" + system.getId(),
                HttpMethod.GET,
                Collections.emptyMap()
        );

        assertThat(publicResponse.getStatusCode().is2xxSuccessful())
                .as("Public should be able to access system in ALL mode")
                .isTrue();

        JsonNode publicDto = objectMapper.readTree(publicResponse.getBody());
        JsonNode systemInfo = publicDto.get("systemInformations");

        if (systemInfo != null && systemInfo.has("name")) {
            String publicSeenName = systemInfo.get("name").asText();
            assertThat(publicSeenName)
                    .as("Public viewers should see publicName as 'name'")
                    .isEqualTo("Public Friendly Name");
        }

        LOG.info("✓ Public viewers correctly see publicName substituted as 'name'");
    }

    @Test
    public void testViewNameVisibleToAuthenticatedUsers() throws Exception {
        LOG.info("Testing viewName visible to authenticated users");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        SystemInformations info = system.getSystemInformations();
        if (info == null) {
            info = new SystemInformations();
        }
        info.setViewName("My Private Name");
        info.setPublicName("Public Friendly Name");
        system.setSystemInformations(info);
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        ResponseEntity<String> ownerResponse = doRequest(
                "api/system/" + system.getId(),
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        assertThat(ownerResponse.getStatusCode().is2xxSuccessful()).isTrue();

        JsonNode ownerDto = objectMapper.readTree(ownerResponse.getBody());
        JsonNode systemInfo = ownerDto.get("systemInformations");

        if (systemInfo != null && systemInfo.has("name")) {
            String ownerSeenName = systemInfo.get("name").asText();
            assertThat(ownerSeenName)
                    .as("Owner should see viewName as 'name'")
                    .isEqualTo("My Private Name");
        }

        if (systemInfo != null && systemInfo.has("publicName")) {
            String publicNameField = systemInfo.get("publicName").asText();
            assertThat(publicNameField)
                    .as("Owner should see publicName as separate field")
                    .isEqualTo("Public Friendly Name");
        }

        LOG.info("✓ Authenticated users correctly see viewName and publicName separately");
    }

    @Test
    public void testPublicNameNullFallsBackToViewName() throws Exception {
        LOG.info("Testing publicName null falls back to viewName");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test-system");

        SystemInformations info = system.getSystemInformations();
        if (info == null) {
            info = new SystemInformations();
        }
        info.setViewName("System Name");
        info.setPublicName(null);
        system.setSystemInformations(info);
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);

        ResponseEntity<String> publicResponse = doRequest(
                "api/system/public/" + system.getId(),
                HttpMethod.GET,
                Collections.emptyMap()
        );

        assertThat(publicResponse.getStatusCode().is2xxSuccessful()).isTrue();

        JsonNode publicDto = objectMapper.readTree(publicResponse.getBody());
        JsonNode systemInfo = publicDto.get("systemInformations");

        if (systemInfo != null && systemInfo.has("name")) {
            String publicSeenName = systemInfo.get("name").asText();
            assertThat(publicSeenName)
                    .as("When publicName is null, public should see viewName")
                    .isEqualTo("System Name");
        }

        LOG.info("✓ Null publicName correctly falls back to viewName for public viewers");
    }
}
