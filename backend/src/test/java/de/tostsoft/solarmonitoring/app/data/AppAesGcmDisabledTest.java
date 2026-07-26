package de.tostsoft.solarmonitoring.app.data;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = {SolarmonitoringApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class AppAesGcmDisabledTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void AesGcmEndpointDisabled() {
        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("X-Nonce", "000102030405060708090a0b");
        var entity = new HttpEntity<>(new byte[32], headers);

        var ex = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.exchange(
                        "http://localhost:" + getServerPort() + "/api/solar/data/aes-gcm?systemId=any-id",
                        HttpMethod.POST, entity, String.class));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
