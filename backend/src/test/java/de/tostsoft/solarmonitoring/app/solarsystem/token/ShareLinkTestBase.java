package de.tostsoft.solarmonitoring.app.solarsystem.token;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink.ShareLinkCreateDTO;
import de.tostsoft.solarmonitoring.lib.model.ShareLink;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ShareLinkTestBase extends AppBaseTest {

    protected User owner;
    protected User admin;
    protected User viewer;
    protected SolarSystem system;
    protected String ownerJwt;
    protected String adminJwt;
    protected String viewerJwt;

    @BeforeEach
    public void prepareShareLinkEnvironment() {
        clearDatabase();
        owner = addUser(false, "owner");
        admin = addUser(true, "admin");
        viewer = addUser(false, "viewer");
        system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test");
        // admin also gets manage access: share-link creation is gated by findSystemWithFullAccess first
        addManges(system, admin);
        ownerJwt = signIn("owner");
        adminJwt = signIn("admin");
        viewerJwt = signIn("viewer");
    }

    protected Map<String, String> authHeader(String jwt) {
        return Map.of("Cookie", "jwt=" + jwt);
    }

    protected JsonNode createLink(String jwt, ShareLinkCreateDTO dto) {
        var resp = doRestRequest("api/system/share-links/" + system.getId(), dto, HttpMethod.POST, authHeader(jwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return tree(resp);
    }

    protected JsonNode resolve(String systemIdOrShortener, String secret) {
        var resp = doRestRequest("api/system/public/share/resolve",
                Map.of("systemId", systemIdOrShortener, "secret", secret), HttpMethod.POST);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return tree(resp);
    }

    protected ResponseEntity<String> getPublicSystem(String viewToken) {
        var headers = new java.util.HashMap<String, String>();
        if (viewToken != null) {
            headers.put("X-View-Token", viewToken);
        }
        return doRequest("api/system/public/" + system.getId(), HttpMethod.GET, headers);
    }

    protected ShareLink getLink(String linkId) {
        return shareLinkRepository.findById(linkId).orElseThrow();
    }

    protected SolarSystem getSystem() {
        return solarSystemRepository.findById(system.getId()).orElseThrow();
    }

    protected JsonNode tree(ResponseEntity<String> resp) {
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
