package de.tostsoft.solarmonitoring.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.proxy.controller.SolarDataController;
import de.tostsoft.solarmonitoring.proxy.dtos.MultDataResponseProxyDTO;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSystemRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SolarMonitoringProxy.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProxyMultDataTest extends ProxyBaseRestTest {

    private Logger LOG = LoggerFactory.getLogger(ProxyApiTests.class);

    @Autowired
    private ProxySolarSystemRepository proxySolarSystemRepository;

    @Autowired
    private ProxySolarSampleRepository proxySolarSampleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SolarDataController solarDataController;

    @BeforeEach
    public void prepare() {
        proxySolarSampleRepository.deleteAll();
        proxySolarSystemRepository.deleteAll();
    }

    @Test
    public void MultOk() {

        var system = new ProxySolarSystem();
        system.setToken(passwordEncoder.encode("token"));
        system.setLastUpdate(Instant.now().toEpochMilli());
        system = proxySolarSystemRepository.save(system);
        proxySolarSystemRepository.save(system);

        var sample = new SampleDTO();
        sample.setDuration(30.f);

        doRestRequest("api/solar/data/mult?systemId="+system.getId(),Collections.singletonList(sample), HttpMethod.POST, Collections.singletonMap("clientToken", "token"));

        var all = proxySolarSystemRepository.findAll();
        Assertions.assertThat(all).size().isEqualTo(1);
    }

    @Test
    public void MultNotOk() {

        var system = new ProxySolarSystem();
        system.setToken(passwordEncoder.encode("token"));
        system.setLastUpdate(Instant.now().toEpochMilli());
        system = proxySolarSystemRepository.save(system);
        proxySolarSystemRepository.save(system);
        String systemID = system.getId();

        var sample = new SampleDTO();
        sample.setDuration(30.f);
        sample.setTimestamp(-1000L);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId="+systemID,Collections.singletonList(sample), HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void MutlOkWithOneBrokenOneOk() throws JsonProcessingException {

        var system = new ProxySolarSystem();
        system.setToken(passwordEncoder.encode("token"));
        system.setLastUpdate(Instant.now().toEpochMilli());
        system = proxySolarSystemRepository.save(system);
        proxySolarSystemRepository.save(system);
        String systemID = system.getId();

        var sample1 = new SampleDTO();
        sample1.setDuration(30.f);

        var sample2 = new SampleDTO();
        sample2.setDuration(30.f);
        sample2.setTimestamp(-1000L);

        var res = doRestRequest("api/solar/data/mult?systemId="+systemID, Arrays.asList(sample1,sample2), HttpMethod.POST, Collections.singletonMap("clientToken", "token"));

        var dto = objectMapper.readValue(res.getBody(), MultDataResponseProxyDTO.class);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).size().isEqualTo(1);
        Assertions.assertThat(dto.getInvalidSamplesIndexes()).contains(1);
    }

    @Test
    public void CheckEmptySampleList() {

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId=1234","[]", HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).containsIgnoringCase("Samples list is empty");
    }

    @Test
    public void CheckToManySampleList() {

        var sample = new ArrayList<SampleDTO>();

        for(int i=0;i<=SolarDataController.MAX_MULT_REQUEST_SAMPLES_SIZE;i++){
            var samp = new SampleDTO();
            samp.setDuration(30.f);
            samp.setTimestamp(Instant.now().toEpochMilli()+i*1000*30);
            sample.add(samp);
        }

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId=1234",sample, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));

        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getResponseBodyAsString()).containsIgnoringCase("To many samples for mult request, max is "+SolarDataController.MAX_MULT_REQUEST_SAMPLES_SIZE);
    }

    @Test
    public void CheckMaxSystemSamples() {

        var system = new ProxySolarSystem();
        system.setToken(passwordEncoder.encode("token"));
        system.setLastUpdate(Instant.now().toEpochMilli());
        system = proxySolarSystemRepository.save(system);
        proxySolarSystemRepository.save(system);
        String systemID = system.getId();

        //seperate system having data
        var system2 = new ProxySolarSystem();
        system2.setToken(passwordEncoder.encode("token"));
        system2.setLastUpdate(Instant.now().toEpochMilli());
        system2 = proxySolarSystemRepository.save(system);

        for(int j=0;j<=10;j++){
            var samples = new ArrayList<SampleDTO>();

            for(int i=0;i<10;i++){
                var samp = new SampleDTO();
                samp.setDuration(30.f);
                samp.setTimestamp(Instant.now().toEpochMilli()+j*i*1000*30);
                samples.add(samp);
            }

            if(j==0){
                doRestRequest("api/solar/data/mult?systemId="+system2.getId(),samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));
            }

            if(j < 10){
                doRestRequest("api/solar/data/mult?systemId="+systemID,samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token"));
            }else{
                var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/solar/data/mult?systemId="+systemID,samples, HttpMethod.POST, Collections.singletonMap("clientToken", "token")));
                Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                Assertions.assertThat(ex.getResponseBodyAsString()).containsIgnoringCase("System has to many cached Samples");
            }
            }
    }

}
