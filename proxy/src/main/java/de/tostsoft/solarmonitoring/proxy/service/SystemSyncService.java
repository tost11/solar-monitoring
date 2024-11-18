package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemDTO;
import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemsDTO;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSystemRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class SystemSyncService {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

  private boolean isOnline = false;

  public boolean getIsOnline() {return isOnline;}

  @Value("${proxy.url}")
  private String proxyUrl;

  @Value("${proxy.token}")
  private String proxyToken;

  @Autowired
  private ProxySolarSystemRepository proxySolarSystemRepository;

  @Scheduled(fixedDelay = 1000 * 60 * 5)
  public void syncData(){
    LOG.info("Perform System update");
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.add("user-agent", "solar-proxy");
    headers.set("proxyToken",proxyToken);

    HttpEntity<String> entity = new HttpEntity<>("", headers);
    ResponseEntity<ProxySystemsDTO> res;
    try{
      res = restTemplate.exchange(proxyUrl+"/api/proxy/systems", HttpMethod.GET, entity, ProxySystemsDTO.class);
    }catch (Exception e){
      LOG.debug(e.getMessage());
      LOG.error("Could not get all systems from main application");
      if(e instanceof HttpStatusCodeException statusCodeException){
          LOG.error("Status: " + statusCodeException.getStatusCode() + " Body: " + statusCodeException.getResponseBodyAsString());
      }
      isOnline = false;
      return;
    }
    isOnline = true;

    if(!res.getStatusCode().is2xxSuccessful()){
      LOG.info("Could not get system info: "+res.getStatusCode()+" "+res.getBody());
      return;
    }
    var systems = res.getBody();
    for (ProxySystemDTO system : systems.getSystems()) {
      var sys = proxySolarSystemRepository.findById(system.getId());
      if(sys.isEmpty()){
        proxySolarSystemRepository.save(ProxySolarSystem.builder().deyeSunSerials(system.getDeyeSunSerials()).id(system.getId()).lastUpdate(Instant.now().toEpochMilli()).token(system.getToken()).build());
        LOG.info("Created new proxy system with id: "+system.getId());
      }else{

        sys.get().setLastUpdate(Instant.now().toEpochMilli());

        if(!StringUtils.equals(sys.get().getToken(), system.getToken())) {
          sys.get().setToken(system.getToken());
          LOG.info("Updated token for proxy system with id: " + system.getId());
        }

        var newDeye =  system.getDeyeSunSerials() == null ? new HashSet<Long>():system.getDeyeSunSerials();
        var oldDeye =  sys.get().getDeyeSunSerials() == null ? new HashSet<Long>():sys.get().getDeyeSunSerials();
        if(!newDeye.containsAll(oldDeye) || !oldDeye.containsAll(newDeye)){
          sys.get().setDeyeSunSerials(system.getDeyeSunSerials());
          LOG.info("Updated Deye serials for proxy system with id: " + system.getId());
        }

        proxySolarSystemRepository.save(sys.get());
      }
    }

    LOG.info("Succesfull upated systems");
    //delete all old systems
    proxySolarSystemRepository.deleteAllByIdNotIn(systems.getSystems().stream().map(ProxySystemDTO::getId).toList());
  }
}
