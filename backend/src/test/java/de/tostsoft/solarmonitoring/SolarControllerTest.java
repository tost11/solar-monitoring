package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.controller.SolarSystemController;
import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.GrafanaService;
import de.tostsoft.solarmonitoring.service.UserService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.RestTemplate;


@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = WebEnvironment.RANDOM_PORT)
class SolarControllerTest {
	private static final Logger LOG = LoggerFactory.getLogger(GrafanaService.class);
	@LocalServerPort
	private int randomServerPort;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private SolarSystemRepository solarSystemRepository;
	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private InfluxConnection influxConnection;

	@Value("${grafana.user}")
	private String grafanaUser;

	@Value("${grafana.password}")
	private String grafanaPassword;

	@Value("${proxy.grafana.target.url}")
	private String grafanaUrl;
	@Autowired
	private UserService userService;
	@Autowired
	private GrafanaService grafanaService;
	@Autowired
	private SolarSystemController solarSystemController;

	private SolarSystemType solarSystemType;


	@BeforeEach
	public void init(){
		cleanUPData();
	}

	private SolarSystemDTO creatUserAndSystem(SolarSystemType solarSystemType) {
		UserRegisterDTO user = new UserRegisterDTO("testLogin", "testtest");
		userService.registerUser(user);
		RegisterSolarSystemDTO registerSolarSystemDTO =new RegisterSolarSystemDTO("testSystem",solarSystemType);
		HttpEntity httpEntity = new HttpEntity(user);
		ResponseEntity<UserDTO> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity, UserDTO.class);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("Cookie","jwt="+response.getBody().getJwt());
		httpEntity = new HttpEntity(registerSolarSystemDTO,headers);
		ResponseEntity<SolarSystemDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, SolarSystemDTO.class);

		return responseSystem.getBody();
	}

	private HttpHeaders createHeaders(){
		return new HttpHeaders() {{
			String auth = grafanaUser + ":" + grafanaPassword;
			byte[] encodedAuth = Base64.encodeBase64(
					auth.getBytes(Charset.defaultCharset()));
			String authHeader = "Basic " + new String(encodedAuth);
			set("Authorization", authHeader);
			set("Content-Type","application/json; charset=UTF-8");
		}};
	}

	private void cleanUPData(){
		RestTemplate restTemplate = new RestTemplate();
		String json = "";
		var entity = new HttpEntity<String>(json,createHeaders());
		var list= grafanaService.getFolders();

		for (int i=0;list.getBody().length>i;i++){
			var foldersDTO =  list.getBody()[i];

			grafanaService.deleteFolder(foldersDTO.getUid());

		}

		var userList =restTemplate.exchange(grafanaUrl+"/api/users",HttpMethod.GET,entity, GrafanaUserDTO[].class);
		LOG.info("list of User "+userList.toString());
		for (int i=0;userList.getBody().length>i;i++){
			var grafanaUser =  userList.getBody()[i];
			if (grafanaUser.getLogin().equals("admin")) {
				continue;
			}
			LOG.info("Delete Influx bucket");
			try {
				influxConnection.deleteBucket(grafanaUser.getLogin());
			}catch (Exception e){
				LOG.error(e.toString());
			}
			LOG.info("Grafana User Delete"+grafanaUser.toString());
			grafanaService.deleteUser(grafanaUser.getId());


		}

		solarSystemRepository.deleteAll();
		userRepository.deleteAll();

	}


	@Test
	public void testSelfMadeSolarEndpoint() {
		SolarSystemDTO solarSystemDTO = creatUserAndSystem(solarSystemType.SELFMADE);
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
		System.out.println(dateFormat.format(date));
		SelfMadeSolarSampleDTO body = new SelfMadeSolarSampleDTO(date.getTime(), 10f, 42f, 2, 454f, 5645f, 56, 0, null, 0f, 0f, 0f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", solarSystemDTO.getToken());
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystemDTO.getName());
		String query = "from(bucket: \"generated testLogin\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"_measurement\"] == \"SELFMADE\")"+
				"  |> filter(fn: (r) => r[\"token\"] ==\"" + solarSystemDTO.getToken() + "\")"+
 				"  |> filter(fn: (r) => r[\"_field\"] == \"BatteryAmpere\" or r[\"_field\"] == \"BatteryVoltage\" or r[\"_field\"] == \"BatteryWatt\" or r[\"_field\"] == \"ChargeAmpere\" or r[\"_field\"] == \"ChargeVolt\" or r[\"_field\"] == \"ChargeWatt\" or r[\"_field\"] == \"Duration\")";

		List<FluxTable> tables = influxConnection.getClient().getQueryApi().query(query);
		for (FluxTable fluxTable : tables) {
			List<FluxRecord> records = fluxTable.getRecords();
			for (FluxRecord fluxRecord : records) {
				if(StringUtils.equals(fluxRecord.getField(),"Duration"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getDuration());
				if(StringUtils.equals(fluxRecord.getField(),"ChargeVolt"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeVoltage());
				if(StringUtils.equals(fluxRecord.getField(),"ChargeAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"ChargeWatt")) {
					if (body.getChargeWatt() == null)
						body.setChargeWatt(body.getChargeVoltage() * body.getChargeAmpere());
					assertThat((Float.parseFloat("" + fluxRecord.getValue()))).isEqualTo(body.getChargeWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ChargeTemperature"))
					assertThat(fluxRecord.getValue()).isEqualTo(body.getChargeTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryVoltage"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryVoltage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryWatt")){
						if(body.getBatteryWatt()==null)
							body.setBatteryWatt(body.getBatteryVoltage()*body.getBatteryAmpere());
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"BatteryPercentage"))
					assertThat(fluxRecord.getValue()).isEqualTo(body.getBatteryPercentage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryTemperature"))
					assertThat(fluxRecord.getValue()).isEqualTo(body.getBatteryTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"DeviceTemperature"))
					assertThat(fluxRecord.getValue()).isEqualTo(body.getDeviceTemperature());
			}
		}
		assertThat(tables.get(0).getRecords().get(0).getTime()).isEqualTo(date.toInstant());
			System.out.println(influxConnection.getClient().getQueryApi().query(query));
		}

	@Test
	public void testSelfMadeSolarConsumerDeviceEndpoint() throws Exception {
		SelfMadeSolarSampleConsumptionDeviceDTO body = new SelfMadeSolarSampleConsumptionDeviceDTO(new Date().getTime(),null,0,0,null,null,0,0,null,null,null,0.f,0,null,null);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken","my_token");
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body,headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/device", HttpMethod.POST, entity, String.class);
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

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/inverter", HttpMethod.POST, entity, String.class);
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

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		//TODO test if date are in database for real
	}

}
