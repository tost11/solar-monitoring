package de.tostsoft.solarmonitoring;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiTests extends ApplicationBaseRestTest {

    private Logger LOG = LoggerFactory.getLogger(ApiTests.class);

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private ManagesRepository managesRepository;

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

    @Test
    public void testRandomApiEndpoint(){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/whatever"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Disabled
    public void test() {
        doRestRequest("/api/system/delete/123456789", "{}");
        System.out.println("ok");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "config","system/public/UNKNOWN_ID","system/public","system/UNKNOWN_ID",
            "system","system/all","system/allManager","system/UNKNOWN_ID",
            "system/deleteManager/","system/deleteManager/UNKNOWN_ID",
            "system/deleteManager/UNKNOWN_ID/UNKNOWN_ID","system/statistics",
            "system/statistics/UNKNOWN_ID","system/status","system/status/UNKNOWN_ID",
            "system/public/mult?systemIds=UNKNOWN_ID","system/mult","user"})
    public void testForbiddenGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system/status","system/status/UNKNOWN_ID"})
    public void testForbiddenṔutApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}",HttpMethod.PUT));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system/status","system/status/UNKNOWN_ID","user/notification"})
    public void testForbiddenDeleteApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}",HttpMethod.DELETE));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "config/registration","system","system/edit","/api/system/delete",
            "/api/system/delete/UNKNOWN_ID","system/addManageBy","system/newToken",
            "system/newToken/UNKNOWN_ID","user/admin/edit","user/admin/findUser","user/admin/NO_VALID_USER",
            "user/findUser","user/findUser/NO_VALID_USER","user/notification","user"})
    public void testForbiddenPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "influx/all","influx/latest","influx/statistics/all","influx/statistics/latest",
            "influx/combined/all","influx/combined/latest","influx/combined/statistics/all",
            "influx/combined/statistics/latest","proxy/systems","system/public/mult","status/UNKNOWN_ID",
            "status/UNKNOWN_ID/all"})
    public void testBadGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user/login","solar/data/mult","solar/data/deye",
            "solar/data/proxy","solar/data","status/UNKNOWN_ID",
            "user/register"})
    public void testBadPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"{}"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "system/public/all"})
    public void testOkGetRequests(String path){
        var res = doRequest("/api/" + path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "whatever","actuator","metrics"})
    public void testRandomEndpoint(String path){
        var res = doRequest(path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        //assertThat(res.getBody()).contains("404"); //TODO somehow check content
    }
}
