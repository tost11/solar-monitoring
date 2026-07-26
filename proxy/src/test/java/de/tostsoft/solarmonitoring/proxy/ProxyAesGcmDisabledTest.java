package de.tostsoft.solarmonitoring.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = {SolarMonitoringProxy.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "api.endpoints.aes-gcm.enabled=false"
)
public class ProxyAesGcmDisabledTest extends ProxyBaseRestTest {

    private static final String NONCE_HEX = "000102030405060708090a0b";

    @Test
    public void AesGcmEndpointDisabled() {
        var ex = assertThrows(HttpClientErrorException.class,
                () -> doAesGcmRequest("any-system-id", new byte[32], NONCE_HEX));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
}
