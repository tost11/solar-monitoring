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

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemTokenValidationTest extends AppBaseTest {

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

    // --- CREATE validation tests ---

    @Test
    public void createToken_nameTooShort() {
        var dto = CreateAccessTokenDTO.builder()
                .name("ab")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void createToken_nameTooLong() {
        var dto = CreateAccessTokenDTO.builder()
                .name("a".repeat(31))
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void createToken_nameInvalidChars() {
        var dto = CreateAccessTokenDTO.builder()
                .name("tok@en!")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void createToken_nameNull() {
        var dto = CreateAccessTokenDTO.builder()
                .name(null)
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createToken_purposeNull() {
        var dto = CreateAccessTokenDTO.builder()
                .name("valid-name")
                .purpose(null)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createToken_nameMinBoundary() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("abc")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void createToken_nameMaxBoundary() throws Exception {
        var dto = CreateAccessTokenDTO.builder()
                .name("a".repeat(30))
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();

        var res = doRestRequest("api/system/tokens/" + system.getId(), dto, HttpMethod.POST, authHeader());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- UPDATE validation tests ---

    @Test
    public void updateToken_nameTooShort() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name("ab")
                .regenerateToken(false)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void updateToken_nameTooLong() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name("a".repeat(31))
                .regenerateToken(false)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void updateToken_nameInvalidChars() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name("tok@en!")
                .regenerateToken(false)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("Token name does not match requirements");
    }

    @Test
    public void updateToken_nameNull() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var dto = UpdateAccessTokenDTO.builder()
                .name(null)
                .regenerateToken(false)
                .build();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, dto, HttpMethod.PATCH, authHeader()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
