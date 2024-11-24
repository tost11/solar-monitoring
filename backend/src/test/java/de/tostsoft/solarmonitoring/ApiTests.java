package de.tostsoft.solarmonitoring;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiTests {

    private Logger LOG = LoggerFactory.getLogger(ApiTests.class);


    @Autowired
    private InfluxConnection influxConnection;

    @Autowired

    private UserRepository userRepository;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private ManagesRepository managesRepository;

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    public void prepare() {

        LOG.info("Delete Influx buckets");
        for (Bucket bucket : influxConnection.getBuckets()) {
          if(bucket.getName().startsWith("_")){//skip system buckets
            continue;
          }
          influxConnection.deleteBucket(bucket.getName());
        }
        userRepository.deleteAll();
        solarSystemRepository.deleteAll();
        managesRepository.deleteAll();
    }

    private ResponseEntity<String> doRestRequest(String url,String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        var entity = new HttpEntity<>(body,headers);

        return restTemplate.exchange("http://localhost:" + randomServerPort + "/" + url, HttpMethod.POST,entity,String.class);
    }

    private ResponseEntity<String> doRestRequest(String url){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(headers);

        return restTemplate.exchange("http://localhost:" + randomServerPort + "/" + url, HttpMethod.GET,entity,String.class);
    }

    private ResponseEntity<String> doRequest(String url){
        RestTemplate restTemplate = new RestTemplate();
        var entity = new HttpEntity<>(null);

        return restTemplate.exchange("http://localhost:" + randomServerPort + "/" + url, HttpMethod.GET,entity,String.class);
    }

    @Test
    public void testRandomApiEndpoint(){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/whatever"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    public void testRandomApiEndpointLogin(){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/user/login","{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testRandomApiEndpointConfigRegistration(){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/config/registration","{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testRandomEndpoint(){
        var res = doRequest("/whatever");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        //assertThat(res.getBody()).contains("404"); //TODO somehow check content
    }
}
