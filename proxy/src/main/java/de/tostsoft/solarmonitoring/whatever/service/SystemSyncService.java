package de.tostsoft.solarmonitoring.whatever.service;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemDTO;
import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemsDTO;
import de.tostsoft.solarmonitoring.whatever.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.whatever.repository.ProxySolarSystemRepository;
import java.util.Collections;
import java.util.HashSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SystemSyncService {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

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
    headers.set("proxyToken",proxyToken);

    HttpEntity<String> entity = new HttpEntity<>("body", headers);

    var res = restTemplate.exchange(proxyUrl+"/api/proxy/systems", HttpMethod.GET, entity, ProxySystemsDTO.class);

    if(!res.getStatusCode().is2xxSuccessful()){
      LOG.info("Could not get system info: "+res.getStatusCode()+" "+res.getBody());
      return;
    }
    var systems = res.getBody();
    for (ProxySystemDTO system : systems.getSystems()) {
      var sys = proxySolarSystemRepository.findById(system.getId());
      if(sys.isEmpty()){
        proxySolarSystemRepository.save(ProxySolarSystem.builder().id(system.getId()).token(system.getToken()).build());
        LOG.info("Created new proxy system with id: "+system.getId());
      }else{
        if(!StringUtils.equals(sys.get().getToken(), system.getToken())) {
          proxySolarSystemRepository.updateToken(system.getId(), system.getToken());
          LOG.info("Updated token for proxy system with id: " + system.getId());
        }

        var newDeye =  system.getDeyeSunSerials() == null ? new HashSet<Long>():system.getDeyeSunSerials();
        var oldDeye =  sys.get().getDeyeSunSerials() == null ? new HashSet<Long>():sys.get().getDeyeSunSerials();
        if(!(newDeye.containsAll(oldDeye) && oldDeye.containsAll(newDeye))){
          proxySolarSystemRepository.updateDeyeSerials(system.getId(), system.getDeyeSunSerials());
          LOG.info("Updated Deye serials for proxy system with id: " + system.getId());
        }
      }
    }

    //delete all old systems
    proxySolarSystemRepository.deleteAllByIdNotIn(systems.getSystems().stream().map(ProxySystemDTO::getId).toList());
  }
}
