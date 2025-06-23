package de.tostsoft.solarmonitoring.testlib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.testlib.model.Mail;
import de.tostsoft.solarmonitoring.testlib.model.MailList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class MailhogTestService {

    @Value("${test.mailhog.webui}")
    private String URI;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void deleteAllMessages(){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(headers);

        restTemplate.exchange(URI+"/api/v1/messages", HttpMethod.DELETE,entity,String.class);
    }

    private String extractSingleResult(JsonNode node){
        var it = node.iterator();
        var next = it.next();
        if(it.hasNext()){
            throw new RuntimeException("Node param: "+node.asText()+" hase more than one element");
        }
        return next.asText();
    }

    public MailList fetchMails() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(headers);

        var resp = restTemplate.exchange(URI+"/api/v2/messages", HttpMethod.GET,entity,String.class);

        JsonNode rootNode = objectMapper.readTree(resp.getBody());

        var mailList = new MailList();
        mailList.setSize(rootNode.get("total").asInt());

        rootNode.get("items").iterator().forEachRemaining(e->{
            mailList.getMailList().add(Mail.builder()
                    .content(e.path("Content").path("Body").asText())
                    .subject(extractSingleResult(e.path("Content").path("Headers").path("Subject")))
                    .from(extractSingleResult(e.path("Content").path("Headers").path("From")))
                    .to(extractSingleResult(e.path("Content").path("Headers").path("To")))
                    .build());
        });

        return mailList;
    }
}
