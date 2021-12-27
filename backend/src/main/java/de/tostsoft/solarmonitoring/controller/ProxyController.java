package de.tostsoft.solarmonitoring.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

@Controller("/api/graphs")
public class ProxyController {

    private String server = "localhost";
    private int port = 3000;

    @RequestMapping("/**")
    public ResponseEntity mirrorRest(@RequestBody(required = false) String body,
                                     HttpMethod method, HttpServletRequest request, HttpServletResponse response)
            throws URISyntaxException {
        String requestUrl = request.getRequestURI().replaceFirst("/api/graphs", "");

        URI uri = new URI("http", null, server, port, null, null, null);
        uri = UriComponentsBuilder.fromUri(uri)
                .path(requestUrl)
                .query(request.getQueryString())
                .build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (StringUtils.equalsIgnoreCase(headerName, "Authorization")) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(headerName, "cockie")) {
                continue;
            }
            headers.set(headerName, request.getHeader(headerName));
        }
    /*
    headers.set("Authorization",
        "Bearer eyJrIjoiRVl0U2V0OTQzcDBZU1k0TG0xeFg2T1FmeHlHRzdPNE0iLCJuIjoidGVzdDIiLCJpZCI6MX0=");
*/
        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.exchange(uri, method, httpEntity, String.class);
        } catch (HttpStatusCodeException e) {
            e.printStackTrace();
            return ResponseEntity.status(e.getRawStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        }
    }

}

