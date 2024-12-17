package de.tostsoft.solarmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiTests extends ApplicationBaseRestTest {

    private Logger LOG = LoggerFactory.getLogger(ApiTests.class);

    @BeforeEach
    public void prepare() {
        clearDatabase();
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
            "system/public/mult?systemIds=UNKNOWN_ID","system/mult","user","tags/available",
            "tags"})
    public void testForbiddenGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system/status","system/status/UNKNOWN_ID"})
    public void testForbiddenPutApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"",HttpMethod.PUT));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system/status","system/status/UNKNOWN_ID","user/notification"})
    public void testForbiddenDeleteApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,"",HttpMethod.DELETE));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "config/registration","system","system/edit","/api/system/delete",
            "/api/system/delete/UNKNOWN_ID","system/addManageBy","system/newToken",
            "system/newToken/UNKNOWN_ID","user/admin/edit","user/admin/findUser","user/admin/NO_VALID_USER",
            "user/findUser","user/findUser/NO_VALID_USER","user/notification","user","tags"})
    public void testForbiddenPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,""));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "influx/all","influx/latest","influx/statistics/all","influx/statistics/latest",
            "influx/combined/all","influx/combined/latest","influx/combined/statistics/all",
            "influx/combined/statistics/latest","proxy/systems","system/public/mult","status/UNKNOWN_ID",
            "status/UNKNOWN_ID/all","tags/byIds"})
    public void testBadGetApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user/login","solar/data/mult","solar/data/deye",
            "solar/data/proxy","solar/data","status/UNKNOWN_ID",
            "user/register","system/search"})
    public void testBadPostApiRequest(String path){
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("/api/" + path,""));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /*@ParameterizedTest
    @ValueSource(strings = {})
    public void testOkGetRequests(String path){
        var res = doRequest("/api/" + path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }*/

    @ParameterizedTest
    @ValueSource(strings = {"system/search"})
    public void testOkPostRequests(String path){
        var res = doRestRequest("/api/" + path,"{}",HttpMethod.POST);
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
