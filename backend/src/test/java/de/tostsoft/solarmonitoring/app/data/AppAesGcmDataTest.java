package de.tostsoft.solarmonitoring.app.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.service.AesGcmService;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = {SolarmonitoringApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "api.endpoints.aes-gcm.enabled=true"
)
public class AppAesGcmDataTest extends AppBaseTest {

    @MockBean
    private InfluxTaskService influxTaskService;

    private static final String PLAIN_TOKEN = "token";
    private static final String NONCE_HEX = "000102030405060708090a0b";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    // --- Helpers ---

    private byte[] encrypt(byte[] plaintext, String plainToken, String systemId, String nonceHex) throws Exception {
        byte[] key = hexToBytes(AesGcmService.sha256Hex(plainToken));
        byte[] nonce = hexToBytes(nonceHex);
        byte[] aad = systemId.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    private byte[] encryptSample(SampleDTO sample, String plainToken, String systemId, String nonceHex) throws Exception {
        byte[] plaintext = objectMapper.writeValueAsBytes(sample);
        return encrypt(plaintext, plainToken, systemId, nonceHex);
    }

    private ResponseEntity<String> doAesGcmRequest(String systemId, byte[] body, String nonce) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("X-Nonce", nonce);
        var entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                "http://localhost:" + getServerPort() + "/api/solar/data/aes-gcm?systemId=" + systemId,
                HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity<String> doAesGcmRequestNoSystemId(byte[] body, String nonce) {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("X-Nonce", nonce);
        var entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                "http://localhost:" + getServerPort() + "/api/solar/data/aes-gcm",
                HttpMethod.POST, entity, String.class);
    }

    private SampleDTO createValidSample() {
        var sample = new SampleDTO();
        sample.setDuration(30.f);
        sample.setTimestamp(Instant.now().toEpochMilli());
        return sample;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // --- Tests ---

    @Test
    public void AesGcmOk() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");
        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var res = doAesGcmRequest(system.getId(), body, NONCE_HEX);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void AesGcmWrongToken() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");
        var sample = createValidSample();
        byte[] body = encryptSample(sample, "wrong-token", system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmSystemNotFound() throws Exception {
        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, "nonexistent-id", NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest("nonexistent-id", body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmExpiredToken() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");

        // Expire the DATA_PUSH_ENCRYPTED token
        system = solarSystemRepository.findById(system.getId()).get();
        var tokens = new java.util.ArrayList<>(system.getTokens());
        for (var token : tokens) {
            if (token.getPurpose() == TokenPurpose.DATA_PUSH_ENCRYPTED) {
                token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            }
        }
        solarSystemRepository.updateTokens(system.getId(), tokens);

        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        String systemId = system.getId();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(systemId, body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmNoEncryptedTokens() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");

        // Remove the DATA_PUSH_ENCRYPTED token
        system = solarSystemRepository.findById(system.getId()).get();
        var encryptedToken = system.getTokens().stream()
                .filter(t -> t.getPurpose() == TokenPurpose.DATA_PUSH_ENCRYPTED)
                .findFirst().get();
        solarSystemRepository.removeToken(system.getId(), encryptedToken.getId());

        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        String systemId = system.getId();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(systemId, body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmInvalidJson() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");
        byte[] body = encrypt("not-valid-json{{{".getBytes(StandardCharsets.UTF_8), PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void AesGcmInvalidSampleDto() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");

        // Valid JSON but duration is null (violates @NotNull)
        var sample = new SampleDTO();
        sample.setTimestamp(Instant.now().toEpochMilli());
        // duration not set -> null -> @NotNull violation
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void AesGcmMultipleTokensTryAll() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");

        // Remove the original DATA_PUSH_ENCRYPTED token and add a new one with a different key
        system = solarSystemRepository.findById(system.getId()).get();
        var encryptedToken = system.getTokens().stream()
                .filter(t -> t.getPurpose() == TokenPurpose.DATA_PUSH_ENCRYPTED)
                .findFirst().get();
        solarSystemRepository.removeToken(system.getId(), encryptedToken.getId());

        // Add two tokens: first one has wrong key, second has "second-token" key
        var wrongToken = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("wrong-key")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex("completely-wrong-key"))
                .createdAt(LocalDateTime.now())
                .build();
        solarSystemRepository.addToken(system.getId(), wrongToken);

        var correctToken = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("second-token")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex("second-token"))
                .createdAt(LocalDateTime.now())
                .build();
        solarSystemRepository.addToken(system.getId(), correctToken);

        var sample = createValidSample();
        byte[] body = encryptSample(sample, "second-token", system.getId(), NONCE_HEX);

        var res = doAesGcmRequest(system.getId(), body, NONCE_HEX);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void AesGcmMissingSystemIdParam() throws Exception {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequestNoSystemId(new byte[32], NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void AesGcmDayLimitReached() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");

        // Set max samples on day to 1
        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(1L);
        system = solarSystemRepository.save(system);

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);
        long noonMillis = noon.toInstant(ZoneOffset.UTC).toEpochMilli();

        // Push one sample via REST to fill the day limit
        var restSample = new SampleDTO();
        restSample.setDuration(30.f);
        restSample.setTimestamp(noonMillis);
        pushDataSample(system.getId(), restSample);

        // Now try via AES-GCM - should be rejected due to day limit
        var aesSample = new SampleDTO();
        aesSample.setDuration(30.f);
        aesSample.setTimestamp(noonMillis + 30000L);
        byte[] body = encryptSample(aesSample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        String systemId = system.getId();
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(systemId, body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAsString()).contains("limit of samples on this day is reached");
    }

    @Test
    public void AesGcmWrongNonceLength() throws Exception {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID, "test");
        var sample = createValidSample();

        // Encrypt with correct 12-byte nonce but send a truncated 8-byte (16 hex char) nonce in header
        // This will cause decryption to fail because the nonce won't match
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);
        String wrongNonce = "0001020304050607"; // only 8 bytes (16 hex chars)

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, wrongNonce));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
