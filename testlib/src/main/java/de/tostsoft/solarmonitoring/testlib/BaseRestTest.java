package de.tostsoft.solarmonitoring.testlib;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

public abstract class BaseRestTest {

    protected abstract int getServerPort();

    protected ResponseEntity<String> doRestRequest(String url, String body) {
        return doRestRequest(url,body, HttpMethod.POST);
    }

    protected ResponseEntity<String> doRestRequest(String url,String body,HttpMethod method) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        var entity = new HttpEntity<>(body,headers);

        return restTemplate.exchange("http://localhost:" + getServerPort() + "/" + url, method,entity,String.class);
    }

    protected ResponseEntity<String> doRestRequest(String url){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(headers);

        return restTemplate.exchange("http://localhost:" + getServerPort() + "/" + url, HttpMethod.GET,entity,String.class);
    }

    protected ResponseEntity<String> doRequest(String url){
        RestTemplate restTemplate = new RestTemplate();
        var entity = new HttpEntity<>(null);

        return restTemplate.exchange("http://localhost:" + getServerPort() + "/" + url, HttpMethod.GET,entity,String.class);
    }

}
