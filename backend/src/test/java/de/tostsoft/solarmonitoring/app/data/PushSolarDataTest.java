package de.tostsoft.solarmonitoring.app.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataConverter;
import de.tostsoft.solarmonitoring.app.dtos.MultDataResponseDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Field;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PushSolarDataTest extends AppBaseTest {

    @Autowired
    private SolarDataConverter solarDataConverter;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Value("${api.tokens.proxy:}")
    private String proxyEndpointApiToken;

    @Test
    public void checkValidationNoSystemID(){

        SampleDTO dto = new SampleDTO();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data",dto, HttpMethod.POST));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Required request parameter 'systemId' for method parameter type String is not present");
    }

    @Test
    public void checkValidationEmptyDTO1(){

        SampleDTO dto = new SampleDTO();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId=NO_VALID_ID",dto, HttpMethod.POST));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Invalid request because of: duration");
    }

    @Test
    public void checkValidationTokenMissing(){

        SampleDTO dto = new SampleDTO();

        dto.setDuration(10.f);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId=NO_VALID_ID",dto, HttpMethod.POST));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Required request header 'clientToken' for method parameter type String is not present");
    }

    @Test
    public void checkValidationEmptyDTOTokenWrong(){

        SampleDTO dto = new SampleDTO();

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        dto.setDuration(10.f);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId="+system.getId(),dto, HttpMethod.POST, Collections.singletonMap("clientToken", "NO_VALID_TOKEN")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"duration","inputWatt","inputVoltageDC",
            "inputAmpereDC","inputWattDC","inputVoltageAC",
            "inputAmpereAC","inputWattAC","outputWatt",
            "outputVoltageDC","outputAmpereDC","outputWattDC",
            "outputVoltageAC","outputAmpereAC","outputWattAC",
            "outputFrequency","inputFrequency","batteryVoltage",
            "batteryPercentage","inputTotalKWH","outputTotalKWH",
            "inputDCTotalKWH","outputDCTotalKWH","inputACTotalKWH",
            "outputACTotalKWH","batteryTotalKWH","totalOH",
            "gridTotalConsumedKWH","gridTotalFeedInKWH"})
    public void checkValidationMinValues(String param) throws NoSuchFieldException, IllegalAccessException {

        SampleDTO dto = new SampleDTO();

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        dto.setDuration(10.f);

        Field field = dto.getClass()
                .getDeclaredField(param);
        field.setAccessible(true);

        field.set(dto, -1.f);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId="+system.getId(),dto, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Invalid request because of: "+param+" <- muss größer-gleich 0 sein");
    }

    @ParameterizedTest
    @ValueSource(strings = {"batteryPercentage"})//check if regex is escaped
    public void checkValidationMaxValues(String param) throws NoSuchFieldException, IllegalAccessException {

        SampleDTO dto = new SampleDTO();

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        dto.setDuration(10.f);

        Field field = dto.getClass()
                .getDeclaredField(param);
        field.setAccessible(true);

        field.set(dto, 99999.f);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId="+system.getId(),dto, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Invalid request because of: "+param+" <- muss kleiner-gleich 100 sein");
    }

    @Test
    public void CheckEmptySampleList() {

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId=1234","[]", HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).containsIgnoringCase("Samples list is empty");
    }

    @Test
    public void checkMultMaxSamples(){

        List<SampleDTO> samples = new ArrayList<>();
        long stamp = System.currentTimeMillis();
        for(int i=0;i<SolarDataConverter.MAX_MULT_REQUEST_SAMPLES_SIZE;i++){
            var samp = new SampleDTO();
            samp.setTimestamp(stamp);
            samp.setDuration(30.f);

            samples.add(samp);

            stamp += 1000 * 30;
        }

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        doRestRequest("api/solar/data/mult?systemId="+system.getId(),samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));

        samples.clear();

        for(int i=0;i<SolarDataConverter.MAX_MULT_REQUEST_SAMPLES_SIZE+1;i++){
            var samp = new SampleDTO();
            samp.setTimestamp(stamp);
            samp.setDuration(30.f);

            samples.add(samp);

            stamp += 1000 * 30;
        }

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId="+system.getId(),samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("To many sample for mult request, max is "+SolarDataConverter.MAX_MULT_REQUEST_SAMPLES_SIZE);
    }

    @Test
    public void checkMaxSamplesOnDay() throws JsonProcessingException {

        long SAMPLES_MAX = 10;

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(SAMPLES_MAX);
        system = solarSystemRepository.save(system);
        String systemId = system.getId();

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);

        List<SampleDTO> samples = new ArrayList<>();
        SampleDTO sampleToMuch = new SampleDTO();

        sampleToMuch.setTimestamp(noon.toInstant(ZoneOffset.UTC).toEpochMilli());
        sampleToMuch.setDuration(30.f);

        for(int i=0;i<10;i++){
            var samp = new SampleDTO();
            samp.setTimestamp(noon.toInstant(ZoneOffset.UTC).toEpochMilli() + (i+1) * 30 * 1000);
            samp.setDuration(30.f);
            samples.add(samp);
        }

        for (SampleDTO sample : samples) {
            doRestRequest("api/solar/data?systemId="+systemId,sample, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));
        }

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data?systemId="+systemId,sampleToMuch, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).contains("Request not handled because limit of samples on this day is reached");

        ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId="+systemId,Collections.singletonList(sampleToMuch), HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var dto = objectMapper.readValue(ex.getResponseBodyAsString(), MultDataResponseDTO.class);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(0);
    }

    @Test
    public void checkMaxSamplesOnDayMult() throws JsonProcessingException {

        long SAMPLES_MAX = 6;

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(SAMPLES_MAX);
        system = solarSystemRepository.save(system);
        String systemId = system.getId();

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);

        List<SampleDTO> samples = new ArrayList<>();

        for(int i=0;i<10;i++){
            var samp = new SampleDTO();
            samp.setTimestamp(noon.toInstant(ZoneOffset.UTC).toEpochMilli() + (i+1) * 30 * 1000);
            samp.setDuration(30.f);
            samples.add(samp);
        }

        samples.get(0).setTimestamp(-1000L);//invalid value;
        samples.get(1).setBatteryPercentage(-10f);//invalid value;

        var res = doRestRequest("api/solar/data/mult?systemId="+systemId,samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));

        ObjectMapper mapper = new ObjectMapper();
        var dto = objectMapper.readValue(res.getBody(), MultDataResponseDTO.class);

        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(0);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(1);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(8);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(9);
    }

    @Test
    public void checkMaxSamplesOnDayMultMultipleDays() throws JsonProcessingException {

        long SAMPLES_MAX = 1;

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(SAMPLES_MAX);
        system = solarSystemRepository.save(system);
        String systemId = system.getId();

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);

        List<SampleDTO> samples = new ArrayList<>();

        for(int i=0;i<3;i++){
            for(int j=0;j<3;j++){
                var samp = new SampleDTO();
                samp.setTimestamp(noon.toInstant(ZoneOffset.UTC).minus(i, ChronoUnit.DAYS).toEpochMilli() + (j+1) * 30 * 1000);
                samp.setDuration(30.f);
                samples.add(samp);
                if(j==0){
                    samp.setBatteryPercentage(-10f);
                }
            }
        }

        var res = doRestRequest("api/solar/data/mult?systemId="+systemId,samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));

        var dto = objectMapper.readValue(res.getBody(), MultDataResponseDTO.class);

        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(0);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(3);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(6);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(2);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(5);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(8);
    }

    @Test
    public void wrongProxyToken() throws JsonProcessingException {
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/proxy?systemId=12345","[]", HttpMethod.POST, Collections.singletonMap("proxyToken", "INVALID_TOKEN")));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void checkMaxSamplesOnDayProxy() throws JsonProcessingException {

        long SAMPLES_MAX = 6;

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(SAMPLES_MAX);
        system = solarSystemRepository.save(system);
        String systemId = system.getId();

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);

        List<SampleDTO> samples = new ArrayList<>();

        for(int i=0;i<10;i++){
            var samp = new SampleDTO();
            samp.setTimestamp(noon.toInstant(ZoneOffset.UTC).toEpochMilli() + (i+1) * 30 * 1000);
            samp.setDuration(30.f);
            samples.add(samp);
        }

        samples.get(0).setTimestamp(-1000L);//invalid value;
        samples.get(1).setBatteryPercentage(-10f);//invalid value;

        var res = doRestRequest("api/solar/data/proxy?systemId="+systemId,samples, HttpMethod.POST, Collections.singletonMap("proxyToken", proxyEndpointApiToken));

        var dto = objectMapper.readValue(res.getBody(), MultDataResponseDTO.class);

        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(0);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(1);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(8);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(9);
    }


    @Test
    public void checkMaxSamplesOnDayMultMultipleDaysProxy() throws JsonProcessingException {

        long SAMPLES_MAX = 1;

        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");

        system = solarSystemRepository.findById(system.getId()).get();
        system.setMaxSamplesOnDay(SAMPLES_MAX);
        system = solarSystemRepository.save(system);
        String systemId = system.getId();

        ZoneId zone = ZoneId.of(system.getTimezone() != null ? system.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime noon = LocalDateTime.of(today, LocalTime.NOON);

        List<SampleDTO> samples = new ArrayList<>();

        for(int i=0;i<3;i++){
            for(int j=0;j<3;j++){
                var samp = new SampleDTO();
                samp.setTimestamp(noon.toInstant(ZoneOffset.UTC).minus(i, ChronoUnit.DAYS).toEpochMilli() + (j+1) * 30 * 1000);
                samp.setDuration(30.f);
                samples.add(samp);
                if(j==0){
                    samp.setBatteryPercentage(-10f);
                }
            }
        }

        var res = doRestRequest("api/solar/data/proxy?systemId="+systemId,samples, HttpMethod.POST, Collections.singletonMap("proxyToken", proxyEndpointApiToken));

        var dto = objectMapper.readValue(res.getBody(), MultDataResponseDTO.class);

        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(0);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(3);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(6);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(2);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(5);
        Assertions.assertThat(dto.getDailyLimitReachedIndexes()).contains(8);
    }
  }
