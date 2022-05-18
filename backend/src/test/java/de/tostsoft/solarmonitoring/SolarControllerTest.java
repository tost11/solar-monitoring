package de.tostsoft.solarmonitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionBothDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionInverterDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.UserService;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import org.springframework.test.context.ActiveProfiles;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("debug")
class SolarControllerTest {
	private static final Logger LOG = LoggerFactory.getLogger(SolarControllerTest.class);
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
	@Value("${debug.token}")
	private String token;
	@Autowired
	private UserService userService;

	@Autowired
	private DebugService debugService;

	@BeforeAll
	public void setup() {
		cleanUpData();
		User user=debugService.crateTestUserWithSystem();
	}

	private RegisterSolarSystemResponseDTO creatUserAndSystem(SolarSystemType solarSystemType) {
		UserRegisterDTO user = new UserRegisterDTO("testLogin", "testtest");
		userService.registerUser(user);
		RegisterSolarSystemDTO registerSolarSystemDTO = RegisterSolarSystemDTO.builder().name("testSystem").type(solarSystemType).maxSolarVoltage(60).build();
		HttpEntity httpEntity = new HttpEntity(user);
		ResponseEntity<UserDTO> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/user/login", HttpMethod.POST, httpEntity, UserDTO.class);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("Cookie", "jwt=" + response.getBody().getJwt());
		httpEntity = new HttpEntity(registerSolarSystemDTO, headers);
		ResponseEntity<RegisterSolarSystemResponseDTO> responseSystem = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/system", HttpMethod.POST, httpEntity, RegisterSolarSystemResponseDTO.class);

		return responseSystem.getBody();
	}

	private void cleanUpData() {

		//TOTO fix that here
		/*
		LOG.info("Delete Influx bucket");
		try {
			influxConnection.deleteBucket(grafanaUser.getLogin());
		}catch (Exception e){
			LOG.error(e.toString());
		}*/

		solarSystemRepository.deleteAll();
		userRepository.deleteAll();
	}


	@Test
	public void testSelfMadeSolarEndpoint() {
		User user = userRepository.findByNameIgnoreCase("debug");
		SolarSystem solarSystem = solarSystemRepository.findAllByType(SolarSystemType.SELFMADE).get(0);
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
		System.out.println(dateFormat.format(date));
		SelfMadeSolarSampleDTO body = new SelfMadeSolarSampleDTO(date.getTime(),10.f, 42f, 2.f, 454f, 5645f, 56.f, 0.f, 0f, 0f, 0f, 0f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", token);

		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystem.getName());
		String query = "from(bucket: \"user-"+user.getId()+"\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"type\"] == \"SELFMADE\")"+
 				"  |> filter(fn: (r) => r[\"_field\"] == \"BatteryAmpere\" or r[\"_field\"] == \"BatteryTemperature\" or r[\"_field\"] == \"BatteryVoltage\" or r[\"_field\"] == \"BatteryWatt\" or r[\"_field\"] == \"ChargeAmpere\" or r[\"_field\"] == \"ChargeTemperature\" or r[\"_field\"] == \"ChargeVolt\" or r[\"_field\"] == \"ChargeWatt\" or r[\"_field\"] == \"ConsumptionDeviceAmpere\" or r[\"_field\"] == \"ConsumptionDeviceVoltage\" or r[\"_field\"] == \"ConsumptionDeviceWatt\" or r[\"_field\"] == \"ConsumptionInverterAmpere\" or r[\"_field\"] == \"ConsumptionInverterVoltage\" or r[\"_field\"] == \"ConsumptionInverterWatt\" or r[\"_field\"] == \"DeviceTemperature\" or r[\"_field\"] == \"Duration\" or r[\"_field\"] == \"TotalConsumption\")";

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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeTemperature());
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryPercentage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"DeviceTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getDeviceTemperature());
			}
		}
		assertThat(tables.get(0).getRecords().get(0).getTime()).isEqualTo(date.toInstant());
			System.out.println(influxConnection.getClient().getQueryApi().query(query));
		}

	@Test
	public void testSelfMadeSolarConsumerDeviceEndpoint() throws Exception {
		User user = userRepository.findByNameIgnoreCase("debug");
		SolarSystem solarSystem = solarSystemRepository.findAllByType(SolarSystemType.SELFMADE).get(0);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionDeviceDTO body = new SelfMadeSolarSampleConsumptionDeviceDTO(date.getTime(),10f,10.f,20.f,null,12f,10.f,30.f,null,10f,20f,null,22.f,null,30f);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", token);
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);
		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/device", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystem.getName());
		String query = "from(bucket: \"user-"+user.getId()+"\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"type\"] == \"SELFMADE_DEVICE\")"+
				"  |> filter(fn: (r) => r[\"_field\"] == \"BatteryAmpere\" or r[\"_field\"] == \"BatteryPercentage\" or r[\"_field\"] == \"BatteryTemperature\" or r[\"_field\"] == \"BatteryVoltage\" or r[\"_field\"] == \"BatteryWatt\" or r[\"_field\"] == \"ChargeAmpere\" or r[\"_field\"] == \"ChargeVolt\" or r[\"_field\"] == \"ChargeWatt\" or r[\"_field\"] == \"TotalConsumption\" or r[\"_field\"] == \"Duration\" or r[\"_field\"] == \"DeviceTemperature\" or r[\"_field\"] == \"ConsumptionWatt\" or r[\"_field\"] == \"ConsumptionVoltage\" or r[\"_field\"] == \"ConsumptionAmpere\")";

		List<FluxTable> tables = influxConnection.getClient().getQueryApi().query(query);

		for (FluxTable fluxTable : tables) {
			List<FluxRecord> records = fluxTable.getRecords();

			for (FluxRecord fluxRecord : records) {
				//Check all Return values and compare that with the input values
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeTemperature());
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryPercentage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryTemperature());

				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionVoltage")) {
					if(body.getConsumptionVoltage()==null)
						body.setConsumptionVoltage(body.getBatteryVoltage());
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionVoltage());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionWatt")) {
					if(body.getConsumptionWatt()==null) {
						if(body.getConsumptionVoltage()==null)
							body.setConsumptionVoltage(body.getBatteryVoltage());
						body.setConsumptionWatt(body.getConsumptionVoltage() * body.getConsumptionAmpere());
					}
						assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"DeviceTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getDeviceTemperature());
			}
		}
		assertThat(tables.get(0).getRecords().get(0).getTime()).isEqualTo(date.toInstant());
		System.out.println(influxConnection.getClient().getQueryApi().query(query));

	}

	@Test
	public void testSelfMadeSolarConsumerInverterEndpoint() throws Exception {
		User user=debugService.crateTestUserWithSystem();
		System.out.println(user.getRelationOwns());
		SolarSystem solarSystem = user.getRelationOwns().get(2);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionInverterDTO body = new SelfMadeSolarSampleConsumptionInverterDTO(date.getTime(),10f,10.f,20.f,null,12f,10.f,30.f,null,10f,20f,null,22.f,null,50f,12f,30f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", token);
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/inverter", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystem.getName());
		String query = "from(bucket: \"user-"+user.getId()+"\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"type\"] == \"SELFMADE_INVERTER\")"+
				"  |> filter(fn: (r) => r[\"_field\"] == \"BatteryAmpere\" or r[\"_field\"] == \"BatteryPercentage\" or r[\"_field\"] == \"BatteryTemperature\" or r[\"_field\"] == \"BatteryVoltage\" or r[\"_field\"] == \"BatteryWatt\" or r[\"_field\"] == \"ChargeAmpere\" or r[\"_field\"] == \"ChargeVolt\" or r[\"_field\"] == \"ChargeWatt\" or r[\"_field\"] == \"ConsumptionInverterAmpere\" or r[\"_field\"] == \"ConsumptionInverterVoltage\" or r[\"_field\"] == \"ConsumptionInverterWatt\" or r[\"_field\"] == \"DeviceTemperature\" or r[\"_field\"] == \"Duration\" or r[\"_field\"] == \"InverterTemperature\" or r[\"_field\"] == \"TotalConsumption\")";

		List<FluxTable> tables = influxConnection.getClient().getQueryApi().query(query);

		for (FluxTable fluxTable : tables) {
			List<FluxRecord> records = fluxTable.getRecords();

			for (FluxRecord fluxRecord : records) {
				//Check all Return values and compare that with the input values
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeTemperature());
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryPercentage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterVoltage")) {
					if(body.getConsumptionInverterVoltage()==null)
						body.setConsumptionInverterVoltage(230f);
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterVoltage());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterWatt")) {
					if(body.getConsumptionInverterWatt()==null) {
						if(body.getConsumptionInverterVoltage()==null)
							body.setConsumptionInverterVoltage(body.getBatteryVoltage());
						body.setConsumptionInverterWatt(body.getConsumptionInverterVoltage() * body.getConsumptionInverterAmpere());
					}
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"InverterTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getInverterTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"DeviceTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getDeviceTemperature());
			}
		}
		assertThat(tables.get(0).getRecords().get(0).getTime()).isEqualTo(date.toInstant());
		System.out.println(influxConnection.getClient().getQueryApi().query(query));
	}

	@Test
	public void testSelfMadeSolarConsumerBothEndpoint() throws Exception {
		User user=debugService.crateTestUserWithSystem();
		System.out.println(user.getRelationOwns());
		SolarSystem solarSystem = user.getRelationOwns().get(3);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionBothDTO body = new SelfMadeSolarSampleConsumptionBothDTO(date.getTime(),10f,20.f,10.f,200f,10f,10f,20.f,null,0f,21f,62f,54f,0f,null,0f,0f,50f,15f,62f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", token);
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);


		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystem.getName());
		String query = "from(bucket: \"user-"+user.getId()+"\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"type\"] == \"SELFMADE_CONSUMPTION\")"+
				"  |> filter(fn: (r) => r[\"_field\"] == \"BatteryAmpere\" or r[\"_field\"] == \"BatteryPercentage\" or r[\"_field\"] == \"BatteryTemperature\" or r[\"_field\"] == \"BatteryVoltage\" or r[\"_field\"] == \"BatteryWatt\" or r[\"_field\"] == \"ChargeAmpere\" or r[\"_field\"] == \"ChargeVolt\" or r[\"_field\"] == \"ChargeWatt\" or r[\"_field\"] == \"ConsumptionAmpere\" or r[\"_field\"] == \"ConsumptionInverterAmpere\" or r[\"_field\"] == \"ConsumptionInverterVoltage\" or r[\"_field\"] == \"ConsumptionInverterWatt\" or r[\"_field\"] == \"ConsumptionVoltage\" or r[\"_field\"] == \"ConsumptionWatt\" or r[\"_field\"] == \"DeviceTemperature\" or r[\"_field\"] == \"Duration\" or r[\"_field\"] == \"InverterTemperature\" or r[\"_field\"] == \"TotalConsumption\")";

		List<FluxTable> tables = influxConnection.getClient().getQueryApi().query(query);
//Check all Return values and compare that with the input values
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getChargeTemperature());
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
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryPercentage());
				if(StringUtils.equals(fluxRecord.getField(),"BatteryTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getBatteryTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionVoltage")) {
					if(body.getConsumptionVoltage()==null)
						body.setConsumptionVoltage(body.getBatteryVoltage());
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionVoltage());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionWatt")) {
					if(body.getConsumptionWatt()==null) {
						if(body.getConsumptionVoltage()==null)
							body.setConsumptionVoltage(body.getBatteryVoltage());
						body.setConsumptionWatt(body.getConsumptionVoltage() * body.getConsumptionAmpere());
					}
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterVoltage")) {
					if(body.getConsumptionInverterVoltage()==null)
						body.setConsumptionInverterVoltage(230f);
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterVoltage());
				}
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterAmpere"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterAmpere());
				if(StringUtils.equals(fluxRecord.getField(),"ConsumptionInverterWatt")) {
					if(body.getConsumptionInverterWatt()==null) {
						if(body.getConsumptionInverterVoltage()==null)
							body.setConsumptionInverterVoltage(body.getBatteryVoltage());
						body.setConsumptionInverterWatt(body.getConsumptionInverterVoltage() * body.getConsumptionInverterAmpere());
					}
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getConsumptionInverterWatt());
				}
				if(StringUtils.equals(fluxRecord.getField(),"InverterTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getInverterTemperature());
				if(StringUtils.equals(fluxRecord.getField(),"DeviceTemperature"))
					assertThat((Float.parseFloat(""+fluxRecord.getValue()))).isEqualTo(body.getDeviceTemperature());
			}
		}
		assertThat(tables.get(0).getRecords().get(0).getTime()).isEqualTo(date.toInstant());
		System.out.println(influxConnection.getClient().getQueryApi().query(query));
	}

}
