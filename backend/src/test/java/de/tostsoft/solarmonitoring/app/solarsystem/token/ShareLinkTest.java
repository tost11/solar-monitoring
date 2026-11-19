package de.tostsoft.solarmonitoring.app.solarsystem.token;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ShareLinkCreateDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.ShareLink;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShareLinkTest extends AppBaseTest {

    private User owner;
    private SolarSystem system;
    private String ownerJwt;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        owner = addUser(false, "owner");
        system = addSolarSystemForUser(owner, SolarSystemType.GRID, "test");
        ownerJwt = signIn("owner");
    }

    private Map<String, String> authHeader(String jwt) {
        return Map.of("Cookie", "jwt=" + jwt);
    }

    private JsonNode createShareLink(ShareLinkCreateDTO dto) {
        var resp = doRestRequest("api/system/share-links/" + system.getId(), dto, HttpMethod.POST, authHeader(ownerJwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode redeem(String code) {
        var resp = doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode tree(org.springframework.http.ResponseEntity<String> resp) {
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void redeem_valid_grantsAccess() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").build());
        var code = link.get("code").asText();
        assertThat(code).isNotBlank();
        assertThat(link.get("url").asText()).contains("/share/" + code);

        var res = redeem(code);
        var password = res.get("password").asText();
        assertThat(password).isNotBlank();
        assertThat(res.get("systemId").asText()).isEqualTo(system.getId());
        assertThat(res.get("level").asText()).isEqualTo("ALL");

        var data = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", password));
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void redeem_maxUsesExhausted_rejected() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").maxUses(1).build());
        var code = link.get("code").asText();

        assertThat(redeem(code).get("password").asText()).isNotBlank();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void redeem_activationWindowExpired_rejected() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").build());
        var code = link.get("code").asText();
        var shareLink = shareLinkRepository.findByCode(code).get();
        shareLink.setActivateBy(LocalDateTime.now().minusHours(1));
        shareLinkRepository.save(shareLink);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void redeem_expired_rejected() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").build());
        var code = link.get("code").asText();
        var shareLink = shareLinkRepository.findByCode(code).get();
        shareLink.setExpiresAt(LocalDateTime.now().minusHours(1));
        shareLinkRepository.save(shareLink);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void redeem_revoked_rejected() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").build());
        var code = link.get("code").asText();
        var linkId = link.get("id").asText();

        var revokeResp = doRequest("api/system/share-links/" + system.getId() + "/" + linkId + "/revoke",
                HttpMethod.POST, authHeader(ownerJwt));
        assertThat(revokeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void redeem_unknownCode_rejected() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/doesnotexist/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void redeem_appliesTokenTtl() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("PRODUCTION").tokenTtlDays(30).build());
        var code = link.get("code").asText();

        var res = redeem(code);
        var password = res.get("password").asText();
        assertThat(res.get("expiresAt").asText()).isNotBlank();

        var data = doRequest("api/system/public/" + system.getId(), HttpMethod.GET, Map.of("X-View-Token", password));
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(data).path("publicFlagOnlyProduction").asBoolean(false)).isTrue();
    }

    @Test
    public void shareLink_listThenDelete() {
        var link = createShareLink(ShareLinkCreateDTO.builder().level("ALL").build());
        var code = link.get("code").asText();
        var linkId = link.get("id").asText();

        var list = doRequest("api/system/share-links/" + system.getId(), HttpMethod.GET, authHeader(ownerJwt));
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(list).toString()).contains(code);

        var del = doRequest("api/system/share-links/" + system.getId() + "/" + linkId, HttpMethod.DELETE, authHeader(ownerJwt));
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRequest("api/system/public/share/" + code + "/redeem", HttpMethod.POST, Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
