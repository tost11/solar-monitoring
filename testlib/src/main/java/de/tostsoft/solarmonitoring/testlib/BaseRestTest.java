package de.tostsoft.solarmonitoring.testlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.runtime.ObjectMethods;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseRestTest {

    protected abstract int getServerPort();

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected ResponseEntity<String> doRestRequest(String url, Object body) {
        return doRestRequest(url,body, HttpMethod.POST);
    }

    protected ResponseEntity<String> doRestRequest(String url, Object body,HttpMethod method) {
        return doRestRequest(url,body, method,new HashMap<>());
    }

    protected ResponseEntity<String> doRestRequest(String url,Object body,HttpMethod method,Map<String,String> setHeaders) {

        String toSend;
        try{
            toSend = objectMapper.writeValueAsString(body);
        }catch (JsonProcessingException e){
            e.printStackTrace();
            toSend = body.toString();
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, String> stringStringEntry : setHeaders.entrySet()) {
            headers.add(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        var entity = new HttpEntity<>(toSend,headers);

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
