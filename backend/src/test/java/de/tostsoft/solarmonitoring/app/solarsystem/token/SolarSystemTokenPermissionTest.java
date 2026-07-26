package de.tostsoft.solarmonitoring.app.solarsystem.token;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CreateAccessTokenDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.UpdateAccessTokenDTO;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemTokenPermissionTest extends AppBaseTest {

    private User owner;
    private User adminUser;
    private User manageUser;
    private User viewUser;
    private SolarSystem system;
    private String ownerJwt;
    private String adminJwt;
    private String manageJwt;
    private String viewJwt;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        owner = addUser(false, "owner");
        adminUser = addUser(false, "admin");
        manageUser = addUser(false, "manager");
        viewUser = addUser(false, "viewer");

        system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test");

        addManager(system, adminUser, Permissions.ADMIN);
        addManager(system, manageUser, Permissions.MANAGE);
        addManager(system, viewUser, Permissions.VIEW);

        ownerJwt = signIn("owner");
        adminJwt = signIn("admin");
        manageJwt = signIn("manager");
        viewJwt = signIn("viewer");
    }

    private void addManager(SolarSystem system, User user, Permissions permission) {
        managesRepository.save(Manages.builder()
                .solarSystem(system)
                .user(user)
                .permission(permission)
                .build());
    }

    private Map<String, String> authHeader(String jwt) {
        return Collections.singletonMap("Cookie", "jwt=" + jwt);
    }

    private String getTokenId(TokenPurpose purpose) {
        var sys = solarSystemRepository.findById(system.getId()).get();
        return sys.getTokens().stream()
                .filter(t -> t.getPurpose() == purpose)
                .findFirst().get().getId();
    }

    private CreateAccessTokenDTO validCreateDto() {
        return CreateAccessTokenDTO.builder()
                .name("test-token")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .build();
    }

    private UpdateAccessTokenDTO validUpdateDto() {
        return UpdateAccessTokenDTO.builder()
                .name("updated-name")
                .regenerateToken(false)
                .build();
    }

    private void assertForbidden(Runnable asManage, Runnable asView) {
        assertThatThrownBy(asManage::run).hasMessageContaining("404");
        assertThatThrownBy(asView::run).hasMessageContaining("404");
    }

    // --- CREATE permission tests ---

    @Test
    public void createToken_ownerAllowed() {
        var res = doRestRequest("api/system/tokens/" + system.getId(), validCreateDto(), HttpMethod.POST, authHeader(ownerJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void createToken_adminManagerAllowed() {
        var res = doRestRequest("api/system/tokens/" + system.getId(), validCreateDto(), HttpMethod.POST, authHeader(adminJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void createToken_manageAndViewForbidden() {
        assertForbidden(
                () -> doRestRequest("api/system/tokens/" + system.getId(), validCreateDto(), HttpMethod.POST, authHeader(manageJwt)),
                () -> doRestRequest("api/system/tokens/" + system.getId(), validCreateDto(), HttpMethod.POST, authHeader(viewJwt))
        );
    }

    @Test
    public void createToken_unauthenticated() {
        assertThatThrownBy(() -> doRestRequest("api/system/tokens/" + system.getId(), validCreateDto(), HttpMethod.POST, Collections.emptyMap()))
                .hasMessageContaining("403");
    }

    // --- DELETE permission tests ---

    @Test
    public void deleteToken_ownerAllowed() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var res = doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, authHeader(ownerJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void deleteToken_adminManagerAllowed() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_ENCRYPTED);
        var res = doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, authHeader(adminJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void deleteToken_manageAndViewForbidden() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        assertForbidden(
                () -> doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, authHeader(manageJwt)),
                () -> doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, authHeader(viewJwt))
        );
    }

    @Test
    public void deleteToken_unauthenticated() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        assertThatThrownBy(() -> doRequest("api/system/tokens/" + system.getId() + "/" + tokenId, HttpMethod.DELETE, Collections.emptyMap()))
                .hasMessageContaining("403");
    }

    // --- UPDATE permission tests ---

    @Test
    public void updateToken_ownerAllowed() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        var res = doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, validUpdateDto(), HttpMethod.PATCH, authHeader(ownerJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateToken_adminManagerAllowed() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_ENCRYPTED);
        var res = doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, validUpdateDto(), HttpMethod.PATCH, authHeader(adminJwt));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateToken_manageAndViewForbidden() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        assertForbidden(
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, validUpdateDto(), HttpMethod.PATCH, authHeader(manageJwt)),
                () -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, validUpdateDto(), HttpMethod.PATCH, authHeader(viewJwt))
        );
    }

    @Test
    public void updateToken_unauthenticated() {
        var tokenId = getTokenId(TokenPurpose.DATA_PUSH_REST);
        assertThatThrownBy(() -> doRestRequest("api/system/tokens/" + system.getId() + "/" + tokenId, validUpdateDto(), HttpMethod.PATCH, Collections.emptyMap()))
                .hasMessageContaining("403");
    }
}
