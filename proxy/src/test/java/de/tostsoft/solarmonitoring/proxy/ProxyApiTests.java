package de.tostsoft.solarmonitoring.proxy;

import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSystemRepository;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SolarMonitoringProxy.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProxyApiTests extends ProxyBaseRestTest {

    private Logger LOG = LoggerFactory.getLogger(ProxyApiTests.class);

    @Autowired
    private ProxySolarSystemRepository proxySolarSystemRepository;

    @Autowired
    private ProxySolarSampleRepository proxySolarSampleRepository;

    @BeforeEach
    public void prepare() {
        proxySolarSampleRepository.deleteAll();
        proxySolarSystemRepository.deleteAll();
    }


    /*
    @ParameterizedTest
    @ValueSource(strings = {})
    public void testForbiddenGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    */

    /*
    @ParameterizedTest
    @ValueSource(strings = {})
    public void testForbiddenPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    */

    /*
    @ParameterizedTest
    @ValueSource(strings = {})
    public void testBadGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    */

    @ParameterizedTest
    @ValueSource(strings = {
            "solar/data/mult","solar/data/deye","solar/data"})
    public void testBadPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /*
    @ParameterizedTest
    @ValueSource(strings = {})
    public void testOkGetRequests(String path){
        var res = doRequest("/api/" + path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    */


    @ParameterizedTest
    @ValueSource(strings = {"whatever","actuator","metrics","api/whatever"})
    public void testRandomEndpoint(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRequest(path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
