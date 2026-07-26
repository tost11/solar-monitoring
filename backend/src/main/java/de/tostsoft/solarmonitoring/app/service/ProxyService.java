package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProxyService {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Autowired
  SolarSystemRepository solarSystemRepository;

  public List<ProxySystemDTO> getSystems(){
    LOG.debug("get all sytsemsd for proxy called");

    var ret = new ArrayList<ProxySystemDTO>();

    Pageable pageableRequest = PageRequest.of(0, 100);
    //iteration over all systems needed because specific query to find only notifications systems is not working because of mongo limitations
    //totally hours wasted here
    Page<SolarSystem> page = solarSystemRepository.findAll(pageableRequest);
    while(true){
      for (SolarSystem solarSystem : page) {
        ret.add(ProxySystemDTO.builder()
            .id(solarSystem.getId())
            .deyeSunSerials(solarSystem.getDeyeSunSerials())
            .tokens(solarSystem.getTokens())
            .build());
      }
      if(!page.hasNext()){
        break;
      }
      page = solarSystemRepository.findAll(page.nextPageable());
    }
    return ret;
  }
}
