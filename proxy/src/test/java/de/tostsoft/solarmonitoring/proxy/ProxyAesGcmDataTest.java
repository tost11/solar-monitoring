package de.tostsoft.solarmonitoring.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySampleDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.service.AesGcmService;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSample;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSystemRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SolarMonitoringProxy.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProxyAesGcmDataTest extends ProxyBaseRestTest {

    @Autowired
    private ProxySolarSystemRepository proxySolarSystemRepository;

    @Autowired
    private ProxySolarSampleRepository proxySolarSampleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PLAIN_TOKEN = "TestToken-AesGcm-123!";
    private static final String NONCE_HEX = "000102030405060708090a0b";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void prepare() {
        proxySolarSampleRepository.deleteAll();
        proxySolarSystemRepository.deleteAll();
    }

    // --- Helpers ---

    private ProxySolarSystem createSystemWithEncryptedToken(String plainToken) {
        var token = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("test-encrypted")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex(plainToken))
                .createdAt(LocalDateTime.now())
                .build();
        var system = ProxySolarSystem.builder()
                .tokens(List.of(token))
                .lastUpdate(Instant.now().toEpochMilli())
                .build();
        return proxySolarSystemRepository.save(system);
    }

    private ProxySolarSystem createSystemWithExpiredToken(String plainToken) {
        var token = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("expired-token")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex(plainToken))
                .createdAt(LocalDateTime.now().minusDays(30))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        var system = ProxySolarSystem.builder()
                .tokens(List.of(token))
                .lastUpdate(Instant.now().toEpochMilli())
                .build();
        return proxySolarSystemRepository.save(system);
    }

    private ProxySolarSystem createSystemWithRestTokenOnly() {
        var token = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("rest-only")
                .purpose(TokenPurpose.DATA_PUSH_REST)
                .hash(passwordEncoder.encode("some-rest-token"))
                .createdAt(LocalDateTime.now())
                .build();
        var system = ProxySolarSystem.builder()
                .tokens(List.of(token))
                .lastUpdate(Instant.now().toEpochMilli())
                .build();
        return proxySolarSystemRepository.save(system);
    }

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
        var system = createSystemWithEncryptedToken(PLAIN_TOKEN);
        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var res = doAesGcmRequest(system.getId(), body, NONCE_HEX);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(proxySolarSampleRepository.countBySystemId(system.getId())).isEqualTo(1);
    }

    @Test
    public void AesGcmWrongToken() throws Exception {
        var system = createSystemWithEncryptedToken(PLAIN_TOKEN);
        var sample = createValidSample();
        // Encrypt with a different token
        byte[] body = encryptSample(sample, "wrong-token-completely", system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmSystemNotFound() throws Exception {
        var sample = createValidSample();
        // Encrypt for a non-existent system
        byte[] body = encryptSample(sample, PLAIN_TOKEN, "nonexistent-id", NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest("nonexistent-id", body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmExpiredToken() throws Exception {
        var system = createSystemWithExpiredToken(PLAIN_TOKEN);
        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmNoEncryptedTokens() throws Exception {
        var system = createSystemWithRestTokenOnly();
        var sample = createValidSample();
        // Encrypt as if we had a valid encrypted token
        byte[] body = encryptSample(sample, "some-rest-token", system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void AesGcmInvalidJson() throws Exception {
        var system = createSystemWithEncryptedToken(PLAIN_TOKEN);
        // Encrypt raw invalid JSON
        byte[] body = encrypt("not-valid-json{{{".getBytes(StandardCharsets.UTF_8), PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void AesGcmInvalidSampleDto() throws Exception {
        var system = createSystemWithEncryptedToken(PLAIN_TOKEN);
        // Valid JSON but duration is null (violates @NotNull)
        var sample = new SampleDTO();
        sample.setTimestamp(Instant.now().toEpochMilli());
        // duration is null -> @NotNull violation
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(system.getId(), body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void AesGcmStaleSystem() throws Exception {
        // Create system with lastUpdate far in the past
        var token = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("test-encrypted")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex(PLAIN_TOKEN))
                .createdAt(LocalDateTime.now())
                .build();
        var system = ProxySolarSystem.builder()
                .tokens(List.of(token))
                .lastUpdate(0L) // epoch start = very old
                .build();
        system = proxySolarSystemRepository.save(system);

        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);
        String systemId = system.getId();

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(systemId, body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void AesGcmBufferFull() throws Exception {
        var system = createSystemWithEncryptedToken(PLAIN_TOKEN);
        String systemId = system.getId();

        // Pre-fill buffer beyond defaultMaxCachedRequests (100 in test config)
        var samplesToInsert = new ArrayList<ProxySolarSample>();
        for (int i = 0; i < 101; i++) {
            var sampleDto = new SampleDTO();
            sampleDto.setDuration(30.f);
            sampleDto.setTimestamp(Instant.now().toEpochMilli() + i * 30000L);
            samplesToInsert.add(ProxySolarSample.builder()
                    .sample(ProxySampleDTO.builder()
                            .systemId(systemId)
                            .sampleDTO(sampleDto)
                            .build())
                    .build());
        }
        proxySolarSampleRepository.saveAll(samplesToInsert);

        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, systemId, NONCE_HEX);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest(systemId, body, NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    public void AesGcmMultipleTokensTryAll() throws Exception {
        // System with two tokens: first has wrong key, second has correct key
        var wrongToken = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("wrong-key-token")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex("completely-different-key"))
                .createdAt(LocalDateTime.now())
                .build();
        var correctToken = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name("correct-token")
                .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                .hash(AesGcmService.sha256Hex(PLAIN_TOKEN))
                .createdAt(LocalDateTime.now())
                .build();
        var system = ProxySolarSystem.builder()
                .tokens(List.of(wrongToken, correctToken))
                .lastUpdate(Instant.now().toEpochMilli())
                .build();
        system = proxySolarSystemRepository.save(system);

        var sample = createValidSample();
        byte[] body = encryptSample(sample, PLAIN_TOKEN, system.getId(), NONCE_HEX);

        var res = doAesGcmRequest(system.getId(), body, NONCE_HEX);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(proxySolarSampleRepository.countBySystemId(system.getId())).isEqualTo(1);
    }
}
