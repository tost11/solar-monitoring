package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.Assertions.assertThat;

import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleDTO;
import java.util.Arrays;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SolarControllerTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;


	@Test
	public void testEndpoint() throws Exception {
		this.restTemplate.getForObject("http://localhost:" + port + "/api/test",String.class);
	}

	@Test
	public void testSelfMadeSolarEndpoint() throws Exception {
		SelfMadeSolarSampleDTO body = new SelfMadeSolarSampleDTO(new Date().getTime(),0,0,null,null,0,0,null,null,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + "/api/solar/data/selfmade", HttpMethod.POST, entity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

	}

}
