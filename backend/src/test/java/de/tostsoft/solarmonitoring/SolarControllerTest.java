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
		SolarSystemDTO solarSystemDTO = creatUserAndSystem(solarSystemType.SELFMADE_DEVICE);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionDeviceDTO body = new SelfMadeSolarSampleConsumptionDeviceDTO(date.getTime(),10f,10,20,null,12f,10,30,null,10f,20f,null,22,null,30f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", solarSystemDTO.getToken());
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);
		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/device", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystemDTO.getName());
		String query = "from(bucket: \"generated testLogin\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"_measurement\"] == \"SELFMADE_DEVICE\")"+
				"  |> filter(fn: (r) => r[\"token\"] ==\"" + solarSystemDTO.getToken() + "\")"+
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
		SolarSystemDTO solarSystemDTO = creatUserAndSystem(solarSystemType.SELFMADE_INVERTER);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionInverterDTO body = new SelfMadeSolarSampleConsumptionInverterDTO(date.getTime(),10f,10,20,null,12f,10,30,null,10f,20f,null,22,null,12f,30f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", solarSystemDTO.getToken());
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);

		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption/inverter", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystemDTO.getName());
		String query = "from(bucket: \"generated testLogin\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"_measurement\"] == \"SELFMADE_INVERTER\")"+
				"  |> filter(fn: (r) => r[\"token\"] ==\"" + solarSystemDTO.getToken() + "\")"+
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
						body.setConsumptionInverterVoltage(body.getBatteryVoltage());
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
		SolarSystemDTO solarSystemDTO = creatUserAndSystem(solarSystemType.SELFMADE_CONSUMPTION);
		Date date = new Date();
		SelfMadeSolarSampleConsumptionBothDTO body = new SelfMadeSolarSampleConsumptionBothDTO(date.getTime(),10f,20,10,200f,10f,10f,20,null,0f,21f,62f,54f,0f,null,0f,0f,15f,62f);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("clientToken", solarSystemDTO.getToken());
		HttpEntity<SelfMadeSolarSampleDTO> entity = new HttpEntity(body, headers);


		ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + randomServerPort + "/api/solar/data/selfmade/consumption", HttpMethod.POST, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		influxConnection.getClient().getBucketsApi().findBucketByName(solarSystemDTO.getName());
		String query = "from(bucket: \"generated testLogin\")" +
				"  |> range(start:0)" +
				"  |> filter(fn: (r) => r[\"_measurement\"] == \"SELFMADE_CONSUMPTION\")"+
				"  |> filter(fn: (r) => r[\"token\"] ==\"" + solarSystemDTO.getToken() + "\")"+
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
