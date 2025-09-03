package de.tostsoft.solarmonitoring.app.data;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataConverter;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Field;
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
            "outputACTotalKWH","batteryTotalKWH","totalOH"})//check if regex is escaped
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
    public void checkMultMaxSamples() throws NoSuchFieldException, IllegalAccessException {

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

  }
