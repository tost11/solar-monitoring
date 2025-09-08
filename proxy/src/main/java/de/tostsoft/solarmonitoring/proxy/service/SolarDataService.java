package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySampleDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSample;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SolarDataService {

  private final Logger LOG = LoggerFactory.getLogger(SolarDataService.class);

  @Autowired
  private ProxySolarSampleRepository proxySolarSampleRepository;

  @Autowired
  private SolarDataSyncService solarDataSyncService;

  @Value("${proxy.directSystems:}")
  private String directProxySystemsConfig;
  private final Set<String> directProxySystems = new HashSet<>();

  @PostConstruct
  public void setup(){
    Collections.addAll(directProxySystems, StringUtils.split(directProxySystemsConfig, ','));
    for (String directProxySystem : directProxySystems) {
      LOG.info("System {} is staged for direct proxying systems", directProxySystem);
    }
  }

  public long getCountOfCurrentSamples(String systemId){
      return proxySolarSampleRepository.countBySystemId(systemId);
  }

  public void addSolarSample(String systemId, List<SampleDTO> samples) {
    if (!directProxySystems.contains(systemId) || !solarDataSyncService.syncEntries(systemId, samples)){
      var listToAdd = new ArrayList<ProxySolarSample>();
      for (SampleDTO sampleDTO : samples) {
        listToAdd.add(ProxySolarSample.builder()
                .sample(ProxySampleDTO.builder()
                        .systemId(systemId)
                        .sampleDTO(sampleDTO)
                        .build())
                .build());
      }
      proxySolarSampleRepository.saveAll(listToAdd);
    }
  }
}
