package de.tostsoft.solarmonitoring.app.solarsystem.token;

import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CreateAccessTokenDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ShareLinkCreateDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemViewTokenTest extends ShareLinkTestBase {

    private String createViewToken(String level) {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level(level).build());
        return resolve(system.getId(), link.get("secret").asText()).get("password").asText();
    }

    // --- single factory path ---

    @Test
    public void directViewTokenCreation_rejected() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("ALL")
                .build();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("access links");
    }

    // --- anonymous access via /api/system/public/{id} (permitAll) ---

    @Test
    public void viewToken_anonymous_grantsAccessToNonPublicSystem() {
        var token = createViewToken("ALL");
        var resp = getPublicSystem(token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void noToken_anonymous_nonPublicSystem_forbidden() {
        var ex = assertThrows(HttpClientErrorException.class, () -> getPublicSystem(null));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void wrongToken_anonymous_forbidden() {
        createViewToken("ALL");
        var ex = assertThrows(HttpClientErrorException.class, () -> getPublicSystem("not-the-right-token"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void expiredViewToken_anonymous_forbidden() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var stored = getLink(link.get("id").asText());
        stored.setTokenExpiresAt(LocalDateTime.now().minusHours(1));
        shareLinkRepository.save(stored);

        var token = resolve(system.getId(), link.get("secret").asText()).get("password").asText();
        var ex = assertThrows(HttpClientErrorException.class, () -> getPublicSystem(token));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- logged-in user without any system access ---

    @Test
    public void viewToken_loggedInUserWithoutAccess_grantsAccess() {
        var token = createViewToken("ALL");
        var resp = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Map.of("Cookie", "jwt=" + viewerJwt, "X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void noToken_loggedInUserWithoutAccess_forbidden() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/" + system.getId(), HttpMethod.GET, Map.of("Cookie", "jwt=" + viewerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- influx data path ---

    @Test
    public void viewToken_influxData_grantsAccess() {
        var token = createViewToken("ALL");
        var resp = doRequest("api/influx/latest?systemId=" + system.getId() + "&duration=5", HttpMethod.GET, Map.of("X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void noToken_influxData_nonPublicSystem_forbidden() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/influx/latest?systemId=" + system.getId() + "&duration=5", HttpMethod.GET, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- token grants more access than the public mode ---

    @Test
    public void allToken_onProductionPublicSystem_grantsMoreThanPublic() {
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        var withoutToken = getPublicSystem(null);
        assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(withoutToken).path("publicFlagOnlyProduction").asBoolean(false)).isTrue();

        var token = createViewToken("ALL");
        var withToken = getPublicSystem(token);
        assertThat(withToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(withToken).path("publicFlagOnlyProduction").asBoolean(false)).isFalse();
    }

    // --- PRODUCTION level grants production-only view on a non-public system ---

    @Test
    public void productionToken_onNonPublicSystem_grantsProductionOnlyView() {
        var token = createViewToken("PRODUCTION");
        var resp = getPublicSystem(token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(resp).path("publicFlagOnlyProduction").asBoolean(false)).isTrue();
    }

    @Test
    public void allToken_onNonPublicSystem_grantsFullView() {
        var token = createViewToken("ALL");
        var resp = getPublicSystem(token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(resp).path("publicFlagOnlyProduction").asBoolean(false)).isFalse();
    }

    // --- POST /api/system/public/tokens/validate (frontend re-open guard) ---

    @Test
    public void validateToken_valid_returnsTrueWithLevel() {
        var token = createViewToken("ALL");
        var resp = doRestRequest("api/system/public/tokens/validate",
                Map.of("systemId", system.getId(), "password", token), HttpMethod.POST);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var node = tree(resp);
        assertThat(node.get("valid").asBoolean()).isTrue();
        assertThat(node.get("level").asText()).isEqualTo("ALL");
    }

    @Test
    public void validateToken_wrongPassword_returnsFalse() {
        createViewToken("ALL");
        var resp = doRestRequest("api/system/public/tokens/validate",
                Map.of("systemId", system.getId(), "password", "wrong-password-1"), HttpMethod.POST);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(resp).get("valid").asBoolean()).isFalse();
    }
}
