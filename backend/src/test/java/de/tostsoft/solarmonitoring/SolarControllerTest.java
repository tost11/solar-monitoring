package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.Assertions.assertThat;

import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionBothDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionInverterDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleDTO;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import java.util.Arrays;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
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

	@Autowired
	private InfluxConnection influxConnection;

	@BeforeEach
	private void init(){

		/*You know this here is a joke...
		  realy just for clearing content of one bucket...
		  and there is no dokumentation how to do it better for influx 2.0...
		  what exactly was the reason for using influx 2.0 ?
		 */

		//TODO fix
		/*
		var api = influxConnection.getClient().getBucketsApi();
		var bucket = api.findBucketByName("my-bucket");
		if(bucket != null){
			api.deleteBucket(bucket);
		}
		String orgId = influxConnection.getClient().getOrganizationsApi().findOrganizations().stream().filter(o->o.getName().equals("my-org")).findFirst().get().getId();
		api.createBucket("my-bucket",orgId);*/
	}

	@Test
	public void testSelfMadeSolarEndpoint() throws Exception {
		SelfMadeSolarSampleDTO body = new SelfMadeSolarSampleDTO(new Date().getTime(),null,0,0,null,null,0,0,null,null,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + "/api/solar/data/selfmade", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//TODO test if date are in database for real
	}

	@Test
	public void testSelfMadeSolarConsumerDeviceEndpoint() throws Exception {
		SelfMadeSolarSampleConsumptionDeviceDTO body = new SelfMadeSolarSampleConsumptionDeviceDTO(new Date().getTime(),null,0,0,null,null,0,0,null,null,null,0.f,0,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + "/api/solar/data/selfmade/consumption/device", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//TODO test if date are in database for real
	}

	@Test
	public void testSelfMadeSolarConsumerInverterEndpoint() throws Exception {
		SelfMadeSolarSampleConsumptionInverterDTO body = new SelfMadeSolarSampleConsumptionInverterDTO(new Date().getTime(),null,0,0,null,null,0,0,null,null,null,null,0.f,0.f,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + "/api/solar/data/selfmade/consumption/inverter", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//TODO test if date are in database for real
	}

	@Test
	public void testSelfMadeSolarConsumerBothEndpoint() throws Exception {
		SelfMadeSolarSampleConsumptionBothDTO body = new SelfMadeSolarSampleConsumptionBothDTO(new Date().getTime(),null,0,0,null,null,0,0,null,null,null,null,0.f,0.f,null,0.f,0.f,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + "/api/solar/data/selfmade/consumption", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//TODO test if date are in database for real
	}

}
