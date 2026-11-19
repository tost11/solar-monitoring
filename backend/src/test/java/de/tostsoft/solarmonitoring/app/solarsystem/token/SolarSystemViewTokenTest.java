package de.tostsoft.solarmonitoring.app.solarsystem.token;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CreateAccessTokenDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemViewTokenTest extends AppBaseTest {

    private static final String VIEW_PASSWORD = "View!Pass1";

    private User owner;
    private User viewer;
    private SolarSystem system;
    private String ownerJwt;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        owner = addUser(false, "owner");
        viewer = addUser(false, "viewer");
        system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test");
        ownerJwt = signIn("owner");
    }

    private Map<String, String> authHeader(String jwt) {
        return Map.of("Cookie", "jwt=" + jwt);
    }

    private String createViewToken(String level) {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo(level)
                .password(VIEW_PASSWORD)
                .build();
        var resp = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody()).get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- CREATE validation ---

    @Test
    public void createViewToken_validLevel_returnsTokenAndAdditionalInfo() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("PRODUCTION")
                .password(VIEW_PASSWORD)
                .build();
        var resp = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode node;
        try {
            node = objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(node.get("token").asText()).isNotBlank();
        assertThat(node.get("purpose").asText()).isEqualTo("VIEW");
        assertThat(node.get("additionalInfo").asText()).isEqualTo("PRODUCTION");
    }

    @Test
    public void createViewToken_missingLevel_rejected() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .build();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("additionalInfo");
    }

    @Test
    public void createViewToken_invalidLevel_rejected() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("MANAGE")
                .build();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("additionalInfo");
    }

    @Test
    public void createViewToken_missingPassword_rejected() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("ALL")
                .build();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("password");
    }

    @Test
    public void createViewToken_invalidPassword_rejected() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("ALL")
                .password("short")
                .build();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("password");
    }

    @Test
    public void createDataPushToken_withAdditionalInfo_ok() {
        var dto = CreateAccessTokenDTO.builder()
                .name("rest-tok")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .additionalInfo("anything-goes")
                .build();
        var resp = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- ANONYMOUS access via /api/system/public/{id} (permitAll) ---

    @Test
    public void viewToken_anonymous_grantsAccessToNonPublicSystem() {
        var token = createViewToken("ALL");
        var resp = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void noToken_anonymous_nonPublicSystem_forbidden() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void wrongToken_anonymous_forbidden() {
        createViewToken("ALL");
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", "not-the-right-token")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void expiredViewToken_anonymous_forbidden() {
        var dto = CreateAccessTokenDTO.builder()
                .name("view-token")
                .purpose(TokenPurpose.VIEW)
                .additionalInfo("ALL")
                .password(VIEW_PASSWORD)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        var resp = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt));
        JsonNode node = objectMapperTree(resp);
        var token = node.get("token").asText();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", token)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- Logged-in user without any system access ---

    @Test
    public void viewToken_loggedInUserWithoutAccess_grantsAccess() {
        var token = createViewToken("ALL");
        var viewerJwt = signIn("viewer");
        var resp = doRequest("api/system/" + system.getId(), HttpMethod.GET,
                Map.of("Cookie", "jwt=" + viewerJwt, "X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void noToken_loggedInUserWithoutAccess_forbidden() {
        var viewerJwt = signIn("viewer");
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/" + system.getId(), HttpMethod.GET, Map.of("Cookie", "jwt=" + viewerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- Influx data path (sysemtToAccesPair) ---

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

    // --- Token grants more access than the public mode ---

    @Test
    public void allToken_onProductionPublicSystem_grantsMoreThanPublic() {
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        var withoutToken = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of());
        assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapperTree(withoutToken).path("publicFlagOnlyProduction").asBoolean(false)).isTrue();

        var token = createViewToken("ALL");
        var withToken = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", token));
        assertThat(withToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapperTree(withToken).path("publicFlagOnlyProduction").asBoolean(false)).isFalse();
    }

    // --- PRODUCTION level grants production-only view on a non-public system ---

    @Test
    public void productionToken_onNonPublicSystem_grantsProductionOnlyView() {
        system.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(system);

        var token = createViewToken("PRODUCTION");
        var resp = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var node = objectMapperTree(resp);
        assertThat(node.path("publicFlagOnlyProduction").asBoolean(false)).isTrue();
    }

    @Test
    public void allToken_onNonPublicSystem_grantsFullView() {
        system.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(system);

        var token = createViewToken("ALL");
        var resp = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", token));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapperTree(resp).path("publicFlagOnlyProduction").asBoolean(false)).isFalse();
    }

    private JsonNode objectMapperTree(org.springframework.http.ResponseEntity<String> resp) {
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
