package de.tostsoft.solarmonitoring.app.solarsystem.token;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CreateAccessTokenDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.UpdateAccessTokenDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemTokenLogicTest extends AppBaseTest {

    private User owner;
    private SolarSystem system;
    private String jwt;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        owner = addUser(false, "owner");
        system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test");
        jwt = signIn("owner");
    }

    private Map<String, String> authHeader() {
        return Collections.singletonMap("Cookie", "jwt=" + jwt);
    }

    private String getTokenId(TokenPurpose purpose) {
        var sys = solarSystemRepository.findById(system.getId()).get();
        return sys.getTokens().stream()
                .filter(t -> t.getPurpose() == purpose)
                .findFirst().get().getId();
    }

    // --- CREATE logic tests ---

    @Test
    public void createToken_responseShape() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("my-rest-token")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = objectMapper.readTree(res.getBody());
        assertThat(json.get("id").asText()).isNotBlank();
        assertThat(json.get("name").asText()).isEqualTo("my-rest-token");
        assertThat(json.get("purpose").asText()).isEqualTo("DATA_PUSH_REST");
        assertThat(json.get("token").asText()).isNotBlank();
        assertThat(json.has("createdAt")).isTrue();
        assertThat(json.get("createdAt")).isNotNull();
        assertThat(json.get("expiresAt").isNull()).isTrue();
    }

    @Test
    public void createToken_aesGcmResponseShape() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("my-aes-token")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = objectMapper.readTree(res.getBody());
        assertThat(json.get("purpose").asText()).isEqualTo("DATA_PUSH_ENCRYPTED");
        assertThat(json.get("token").asText()).isNotBlank();
    }

    @Test
    public void createToken_withExpiry() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("expiring-token")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = objectMapper.readTree(res.getBody());
        assertThat(json.get("expiresAt").isNull()).isFalse();
    }

    @Test
    public void createToken_persistedInDb() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("persisted-token")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());

        var sys = solarSystemRepository.findById(system.getId()).get();
        var match = sys.getTokens().stream()
                .filter(t -> t.getName().equals("persisted-token") && t.getPurpose() == TokenPurpose.DATA_PUSH_REST)
                .findFirst();
        assertThat(match).isPresent();
    }

    // --- DELETE logic tests ---

    @Test
    public void deleteToken_removedFromDb() throws Exception {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);

        var res = doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var sys = solarSystemRepository.findById(system.getId()).get();
        var match = sys.getTokens().stream().filter(t -> t.getId().equals(tokenId)).findFirst();
        assertThat(match).isEmpty();
    }

    @Test
    public void deleteToken_notFound() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/tokens/" + system.getId() + "/nonexistent-id", HttpMethod.DELETE, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- UPDATE logic tests ---

    @Test
    public void updateToken_noRegenerate_responseShape() throws Exception {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name("updated-name")
                .regenerateToken(false)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = objectMapper.readTree(res.getBody());
        assertThat(json.get("id").asText()).isEqualTo(tokenId);
        assertThat(json.get("name").asText()).isEqualTo("updated-name");
        assertThat(json.has("purpose")).isTrue();
        assertThat(json.has("token")).isFalse();
    }

    @Test
    public void updateToken_withRegenerate_responseShape() throws Exception {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name("regenerated")
                .regenerateToken(true)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = objectMapper.readTree(res.getBody());
        assertThat(json.get("name").asText()).isEqualTo("regenerated");
        assertThat(json.has("token")).isTrue();
        assertThat(json.get("token").asText()).isNotBlank();
    }

    @Test
    public void updateToken_noRegenerate_persistedInDb() throws Exception {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var expiry = LocalDateTime.now().plusDays(14);
        var dto = UpdateAccessTokenDTO.builder()
                .name("new-name-db")
                .expiresAt(expiry)
                .regenerateToken(false)
                .build();

        doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader());

        var sys = solarSystemRepository.findById(system.getId()).get();
        var token = sys.getTokens().stream().filter(t -> t.getId().equals(tokenId)).findFirst().get();
        assertThat(token.getName()).isEqualTo("new-name-db");
        assertThat(token.getExpiresAt()).isNotNull();
    }

    @Test
    public void updateToken_expiryCanBeCleared() throws Exception {
        // First, create a token with expiry
        var createDto = CreateAccessTokenDTO.builder()
                .name("expiry-clear-test")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        var createRes = doRestRequest("api/system/tokens/" + system.getId(), createDto, HttpMethod.POST, authHeader());
        var createdId = objectMapper.readTree(createRes.getBody()).get("id").asText();

        // Now update with null expiry
        var updateDto = UpdateAccessTokenDTO.builder()
                .name("expiry-clear-test")
                .expiresAt(null)
                .regenerateToken(false)
                .build();
        doRestRequest("api/system/tokens/" + system.getId() + "/" + createdId, updateDto, HttpMethod.PATCH, authHeader());

        var sys = solarSystemRepository.findById(system.getId()).get();
        var token = sys.getTokens().stream().filter(t -> t.getId().equals(createdId)).findFirst().get();
        assertThat(token.getExpiresAt()).isNull();
    }

    @Test
    public void updateToken_notFound() {
        var dto = UpdateAccessTokenDTO.builder()
                .name("whatever")
                .regenerateToken(false)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/nonexistent-id", dto, HttpMethod.PATCH, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void createMultipleTokens_allPersisted() throws Exception {
        // Create 3 additional tokens
        for (int i = 0; i < 3; i++) {
            var dto = CreateAccessTokenDTO.builder()
                    .name("token-" + i)
                    .purpose(i % 2 == 0 ? TokenPurpose.DATA_PUSH_REST : TokenPurpose.DATA_PUSH_ENCRYPTED)
                    .build();
            doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        }

        // System already had 2 default tokens, now should have 5
        var sys = solarSystemRepository.findById(system.getId()).get();
        assertThat(sys.getTokens()).hasSize(5);
    }
}
