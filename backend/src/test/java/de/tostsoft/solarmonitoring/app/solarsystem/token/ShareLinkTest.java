package de.tostsoft.solarmonitoring.app.solarsystem.token;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink.ShareLinkCreateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShareLinkTest extends ShareLinkTestBase {

    private static final String PAST_DATE = "2020-01-01T00:00:00.000Z";

    // --- CREATE ---

    @Test
    public void create_generated_returnsSecretOnce() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());

        var secret = link.get("secret").asText();
        assertThat(secret).matches("[A-Za-z0-9]{12}");
        assertThat(link.get("manual").asBoolean()).isFalse();
        assertThat(link.get("id").asText()).matches("[0-9a-f]{24}");
        assertThat(link.get("remainingUsages").isNull()).isTrue();
        assertThat(link.get("revoked").asBoolean()).isFalse();

        var list = doRequest("api/system/share-links/" + system.getId(), HttpMethod.GET, authHeader(ownerJwt));
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        var listNode = tree(list);
        assertThat(listNode).hasSize(1);
        assertThat(listNode.get(0).get("id").asText()).isEqualTo(link.get("id").asText());
        assertThat(listNode.get(0).get("secret").isNull()).isTrue();
    }

    @Test
    public void create_manualAsAdmin_ok() {
        var link = createLink(adminJwt, ShareLinkCreateDTO.builder()
                .level("ALL")
                .secret("LinkSecret1!")
                .build());

        assertThat(link.get("manual").asBoolean()).isTrue();
        assertThat(link.get("secret").asText()).isEqualTo("LinkSecret1!");

        var res = resolve(system.getId(), "LinkSecret1!");
        assertThat(res.get("valid").asBoolean()).isTrue();
    }

    @Test
    public void create_manualAsNonAdmin_forbidden() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId(),
                        ShareLinkCreateDTO.builder().level("ALL").secret("LinkSecret1!").build(),
                        HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void create_weakSecret_rejected() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId(),
                        ShareLinkCreateDTO.builder().level("ALL").secret("short").build(),
                        HttpMethod.POST, authHeader(adminJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("secret");
    }

    @Test
    public void create_duplicateSecret_conflict() {
        createLink(adminJwt, ShareLinkCreateDTO.builder().level("ALL").secret("LinkSecret1!").build());

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId(),
                        ShareLinkCreateDTO.builder().level("ALL").secret("LinkSecret1!").build(),
                        HttpMethod.POST, authHeader(adminJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @MethodSource("invalidCreateBodies")
    public void create_invalidInput_rejected(String body, String expectedFragment) {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId(), body, HttpMethod.POST, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains(expectedFragment);
    }

    static Stream<String[]> invalidCreateBodies() {
        return Stream.of(
                new String[]{"{\"level\":\"MANAGE\"}", "level"},
                new String[]{"{}", "level"},
                new String[]{"{\"level\":\"ALL\",\"maxUses\":0}", "maxUses"},
                new String[]{"{\"level\":\"ALL\",\"maxUses\":-5}", "maxUses"},
                new String[]{"{\"level\":\"ALL\",\"expiresAt\":\"" + PAST_DATE + "\"}", "expiresAt"},
                new String[]{"{\"level\":\"ALL\",\"tokenExpiresAt\":\"" + PAST_DATE + "\"}", "tokenExpiresAt"}
        );
    }

    @Test
    public void create_byUserWithoutAccess_notFound() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId(),
                        ShareLinkCreateDTO.builder().level("ALL").build(),
                        HttpMethod.POST, authHeader(viewerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- RESOLVE ---

    @Test
    public void resolve_valid_grantsAccess() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        var res = resolve(system.getId(), secret);
        assertThat(res.get("valid").asBoolean()).isTrue();
        assertThat(res.get("password").asText()).isNotBlank();
        assertThat(res.get("systemId").asText()).isEqualTo(system.getId());
        assertThat(res.get("level").asText()).isEqualTo("ALL");
        assertThat(res.get("name").asText()).isEqualTo("test");

        var data = getPublicSystem(res.get("password").asText());
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void resolve_mintsNewTokenPerUse() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        var first = resolve(system.getId(), secret);
        var second = resolve(system.getId(), secret);
        var firstPassword = first.get("password").asText();
        var secondPassword = second.get("password").asText();
        assertThat(secondPassword).isNotEqualTo(firstPassword);

        assertThat(getPublicSystem(firstPassword).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getPublicSystem(secondPassword).getStatusCode()).isEqualTo(HttpStatus.OK);

        var linkId = link.get("id").asText();
        var viewTokens = getSystem().getTokens().stream()
                .filter(t -> t.getViewPermission() != null && linkId.equals(t.getViewPermission().getLinkId()))
                .toList();
        assertThat(viewTokens).hasSize(2);
    }

    @Test
    public void resolve_wrongSecret_invalid() {
        createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());

        var res = resolve(system.getId(), "000000000000");
        assertThat(res.get("valid").asBoolean()).isFalse();
    }

    @Test
    public void resolve_unknownSystem_invalid() {
        var res = resolve("doesnotexist", "000000000000");
        assertThat(res.get("valid").asBoolean()).isFalse();
    }

    @Test
    public void resolve_expiredLink_invalid() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();
        var stored = getLink(link.get("id").asText());
        stored.setExpiresAt(LocalDateTime.now().minusHours(1));
        shareLinkRepository.save(stored);

        var res = resolve(system.getId(), secret);
        assertThat(res.get("valid").asBoolean()).isFalse();
    }

    @Test
    public void resolve_revokedLink_invalid() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        var revoke = doRequest("api/system/share-links/" + system.getId() + "/" + link.get("id").asText() + "/revoke",
                HttpMethod.POST, authHeader(ownerJwt));
        assertThat(revoke.getStatusCode()).isEqualTo(HttpStatus.OK);

        var res = resolve(system.getId(), secret);
        assertThat(res.get("valid").asBoolean()).isFalse();
    }

    @Test
    public void resolve_exhausted_invalid() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").maxUses(1).build());
        var secret = link.get("secret").asText();

        assertThat(resolve(system.getId(), secret).get("valid").asBoolean()).isTrue();

        var res = resolve(system.getId(), secret);
        assertThat(res.get("valid").asBoolean()).isFalse();

        var list = doRequest("api/system/share-links/" + system.getId(), HttpMethod.GET, authHeader(ownerJwt));
        assertThat(tree(list).get(0).get("remainingUsages").asInt()).isZero();
    }

    @Test
    public void resolve_unlimited_multipleUses() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        for (int i = 0; i < 3; i++) {
            assertThat(resolve(system.getId(), secret).get("valid").asBoolean()).isTrue();
        }
    }

    @Test
    public void resolve_appliesTokenExpiresAt() {
        var tokenExpiry = LocalDateTime.now().plusDays(30);
        var body = "{\"level\":\"ALL\",\"tokenExpiresAt\":\"" + tokenExpiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"}";
        var resp = doRestRequest("api/system/share-links/" + system.getId(), body, HttpMethod.POST, authHeader(ownerJwt));
        var link = tree(resp);

        var res = resolve(system.getId(), link.get("secret").asText());
        assertThat(res.get("valid").asBoolean()).isTrue();

        var returned = LocalDateTime.parse(res.get("expiresAt").asText());
        assertThat(returned.toLocalDate()).isEqualTo(tokenExpiry.toLocalDate());
        assertThat(getPublicSystem(res.get("password").asText()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void resolve_pastTokenExpiresAt_tokenForbidden() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();
        var stored = getLink(link.get("id").asText());
        stored.setTokenExpiresAt(LocalDateTime.now().minusHours(1));
        shareLinkRepository.save(stored);

        var res = resolve(system.getId(), secret);
        assertThat(res.get("valid").asBoolean()).isTrue();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> getPublicSystem(res.get("password").asText()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void resolve_updatesLastRedeemedAt() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        assertThat(getLink(link.get("id").asText()).getLastRedeemedAt()).isNull();

        resolve(system.getId(), secret);

        assertThat(getLink(link.get("id").asText()).getLastRedeemedAt()).isNotNull();
    }

    @Test
    public void resolve_productionLevel_grantsProductionOnlyView() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("PRODUCTION").build());
        var res = resolve(system.getId(), link.get("secret").asText());

        var data = getPublicSystem(res.get("password").asText());
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tree(data).path("publicFlagOnlyProduction").asBoolean(false)).isTrue();
    }

    // --- MANAGEMENT ---

    @Test
    public void revoke_stopsMints_existingTokenStillWorks() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();
        var password = resolve(system.getId(), secret).get("password").asText();

        var revoke = doRequest("api/system/share-links/" + system.getId() + "/" + link.get("id").asText() + "/revoke",
                HttpMethod.POST, authHeader(ownerJwt));
        assertThat(revoke.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(resolve(system.getId(), secret).get("valid").asBoolean()).isFalse();
        assertThat(getPublicSystem(password).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void cleanup_removesLinkAndTokens() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();
        var password = resolve(system.getId(), secret).get("password").asText();
        var linkId = link.get("id").asText();

        var cleanup = doRequest("api/system/share-links/" + system.getId() + "/" + linkId + "/cleanup",
                HttpMethod.POST, authHeader(ownerJwt));
        assertThat(cleanup.getStatusCode()).isEqualTo(HttpStatus.OK);

        var list = doRequest("api/system/share-links/" + system.getId(), HttpMethod.GET, authHeader(ownerJwt));
        assertThat(tree(list)).isEmpty();
        assertThat(resolve(system.getId(), secret).get("valid").asBoolean()).isFalse();
        var ex = assertThrows(HttpClientErrorException.class, () -> getPublicSystem(password));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void deleteSystem_cascadesShareLinks() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var linkId = link.get("id").asText();

        var del = doRequest("api/system/" + system.getId(), HttpMethod.DELETE, authHeader(ownerJwt));
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(shareLinkRepository.findBySystemId(system.getId())).isEmpty();
        assertThat(shareLinkRepository.findById(linkId)).isEmpty();
    }

    @Test
    public void resolve_bruteForce_locksAfterMaxFailedAttempts() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        for (int i = 0; i < 8; i++) {
            assertThat(resolve(system.getId(), "000000000000").get("valid").asBoolean()).isFalse();
        }

        assertThat(accountLockoutRepository.findByUsername("sharelink:" + system.getId())).isPresent();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/public/share/resolve",
                        Map.of("systemId", system.getId(), "secret", secret), HttpMethod.POST));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- UPDATE ---

    private JsonNode updateLink(String jwt, String linkId, String body) {
        var resp = doRestRequest("api/system/share-links/" + system.getId() + "/" + linkId, body, HttpMethod.PATCH, authHeader(jwt));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return tree(resp);
    }

    @Test
    public void update_changesUsagesDatesNotesLevel() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var linkId = link.get("id").asText();
        var tokenExpiry = LocalDateTime.now().plusDays(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        var res = updateLink(ownerJwt, linkId,
                "{\"level\":\"PRODUCTION\",\"maxUses\":7,\"tokenExpiresAt\":\"" + tokenExpiry + "\",\"notes\":\"updated\"}");
        assertThat(res.get("secret").isNull()).isTrue();
        assertThat(res.get("level").asText()).isEqualTo("PRODUCTION");
        assertThat(res.get("remainingUsages").asInt()).isEqualTo(7);
        assertThat(res.get("notes").asText()).isEqualTo("updated");
        assertThat(LocalDateTime.parse(res.get("tokenExpiresAt").asText()).toLocalDate())
                .isEqualTo(LocalDateTime.parse(tokenExpiry).toLocalDate());

        var stored = getLink(linkId);
        assertThat(stored.getLevel()).isEqualTo("PRODUCTION");
        assertThat(stored.getRemainingUsages()).isEqualTo(7);
        assertThat(stored.getNotes()).isEqualTo("updated");
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    public void update_nulls_revertToUnlimitedAndNever() {
        var tokenExpiry = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").maxUses(5).build());
        var linkId = link.get("id").asText();
        updateLink(ownerJwt, linkId, "{\"level\":\"ALL\",\"maxUses\":5,\"tokenExpiresAt\":\"" + tokenExpiry + "\"}");

        var res = updateLink(ownerJwt, linkId, "{\"level\":\"ALL\"}");
        assertThat(res.get("remainingUsages").isNull()).isTrue();
        assertThat(res.get("expiresAt").isNull()).isTrue();
        assertThat(res.get("tokenExpiresAt").isNull()).isTrue();

        var stored = getLink(linkId);
        assertThat(stored.getRemainingUsages()).isNull();
        assertThat(stored.getTokenExpiresAt()).isNull();
    }

    @Test
    public void update_level_affectsOnlyNextMint() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var secret = link.get("secret").asText();

        resolve(system.getId(), secret);
        updateLink(ownerJwt, link.get("id").asText(), "{\"level\":\"PRODUCTION\"}");
        resolve(system.getId(), secret);

        var tokens = getSystem().getTokens().stream()
                .filter(t -> t.getViewPermission() != null && link.get("id").asText().equals(t.getViewPermission().getLinkId()))
                .toList();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).getViewPermission().getPermission().name()).isEqualTo("ALL");
        assertThat(tokens.get(1).getViewPermission().getPermission().name()).isEqualTo("PRODUCTION");
    }

    @Test
    public void update_tokenExpiresAt_existingTokenUnchanged() {
        var oldExpiry = LocalDateTime.now().plusDays(30);
        var tokenExpiry = oldExpiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        updateLink(ownerJwt, link.get("id").asText(), "{\"level\":\"ALL\",\"tokenExpiresAt\":\"" + tokenExpiry + "\"}");

        var res = resolve(system.getId(), link.get("secret").asText());
        assertThat(res.get("valid").asBoolean()).isTrue();

        var newExpiry = LocalDateTime.now().plusDays(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        updateLink(ownerJwt, link.get("id").asText(), "{\"level\":\"ALL\",\"tokenExpiresAt\":\"" + newExpiry + "\"}");

        var token = getSystem().getTokens().stream()
                .filter(t -> t.getViewPermission() != null && link.get("id").asText().equals(t.getViewPermission().getLinkId()))
                .findFirst().orElseThrow();
        assertThat(token.getExpiresAt().toLocalDate()).isEqualTo(oldExpiry.toLocalDate());
    }

    @Test
    public void update_revokedLink_rejected() {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        doRequest("api/system/share-links/" + system.getId() + "/" + link.get("id").asText() + "/revoke",
                HttpMethod.POST, authHeader(ownerJwt));

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId() + "/" + link.get("id").asText(),
                        "{\"level\":\"ALL\"}", HttpMethod.PATCH, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void update_unknownLink_notFound() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId() + "/unknown-link-id",
                        "{\"level\":\"ALL\"}", HttpMethod.PATCH, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateBodies")
    public void update_invalidInput_rejected(String body, String expectedFragment) {
        var link = createLink(ownerJwt, ShareLinkCreateDTO.builder().level("ALL").build());
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("api/system/share-links/" + system.getId() + "/" + link.get("id").asText(),
                        body, HttpMethod.PATCH, authHeader(ownerJwt)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains(expectedFragment);
    }

    static Stream<String[]> invalidUpdateBodies() {
        return Stream.of(
                new String[]{"{\"level\":\"MANAGE\"}", "level"},
                new String[]{"{}", "level"},
                new String[]{"{\"level\":\"ALL\",\"maxUses\":0}", "maxUses"},
                new String[]{"{\"level\":\"ALL\",\"maxUses\":-5}", "maxUses"},
                new String[]{"{\"level\":\"ALL\",\"expiresAt\":\"" + PAST_DATE + "\"}", "expiresAt"},
                new String[]{"{\"level\":\"ALL\",\"tokenExpiresAt\":\"" + PAST_DATE + "\"}", "tokenExpiresAt"}
        );
    }
}
