package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
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

public class SolarSystemPublicModeNoneTest extends AppBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemPublicModeNoneTest.class);

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testPublicModeNoneDeniesPublicAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE denies public access");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        system = solarSystemRepository.save(system);

        ResponseEntity<String> response = doRequest(
                "api/system/public/" + system.getId(),
                HttpMethod.GET,
                Collections.emptyMap()
        );

        assertThat(response.getStatusCode().value())
                .as("Public access to NONE mode system should return 401 or 403")
                .isIn(401, 403, 404);

        LOG.info("✓ PublicMode.NONE correctly denies public access (HTTP {})", response.getStatusCode().value());
    }

    @Test
    public void testPublicModeNoneAllowsOwnerAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE allows owner access");

        User owner = addUser(false, "owner");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        system = solarSystemRepository.save(system);

        String jwt = signIn("owner", "password");
        ResponseEntity<String> response = doRequest(
                "api/system/" + system.getId(),
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Owner should be able to access NONE mode system")
                .isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.has("id")).as("Response should contain system data").isTrue();
        assertThat(root.get("id").asText()).isEqualTo(system.getId());

        LOG.info("✓ PublicMode.NONE correctly allows owner access");
    }

    @Test
    public void testPublicModeNoneAllowsExplicitPermissionAccess() throws Exception {
        LOG.info("Testing PublicMode.NONE allows access for users with explicit permissions");

        User owner = addUser(false, "owner");
        User viewer = addUser(false, "viewer");
        SolarSystem system = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        system.setPublicMode(PublicMode.NONE);
        system = solarSystemRepository.save(system);

        Manages manages = Manages.builder()
                .solarSystem(system)
                .user(viewer)
                .permission(Permissions.VIEW)
                .build();
        managesRepository.save(manages);

        String viewerJwt = signIn("viewer", "password");
        ResponseEntity<String> response = doRequest(
                "api/system/" + system.getId(),
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + viewerJwt)
        );

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("User with VIEW permission should access NONE mode system")
                .isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.get("id").asText()).isEqualTo(system.getId());

        LOG.info("✓ PublicMode.NONE correctly allows access for users with explicit permissions");
    }

    @Test
    public void testPublicModeNoneHidesSystemFromPublicListing() throws Exception {
        LOG.info("Testing PublicMode.NONE hides system from public listings");

        User owner = addUser(false, "owner");

        SolarSystem publicSystem = addSolarSystemForUser(owner, SolarSystemType.GRID, "public-system");
        publicSystem.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(publicSystem);

        SolarSystem privateSystem = addSolarSystemForUser(owner, SolarSystemType.GRID, "private-system");
        privateSystem.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(privateSystem);

        ResponseEntity<String> response = doRequest(
                "api/system/public",
                HttpMethod.GET,
                Collections.emptyMap()
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode systems = objectMapper.readTree(response.getBody());
        assertThat(systems.isArray()).isTrue();

        boolean foundPublic = false;
        boolean foundPrivate = false;
        for (JsonNode system : systems) {
            String id = system.get("id").asText();
            if (id.equals(publicSystem.getId())) {
                foundPublic = true;
            }
            if (id.equals(privateSystem.getId())) {
                foundPrivate = true;
            }
        }

        assertThat(foundPublic).as("Public system should appear in listing").isTrue();
        assertThat(foundPrivate).as("Private (NONE mode) system should NOT appear in listing").isFalse();

        LOG.info("✓ PublicMode.NONE correctly hides system from public listings");
    }
}
