package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySampleDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSample;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolarDataService {

  @Autowired
  private ProxySolarSampleRepository proxySolarSampleRepository;

  public void addSolarSample(String systemId, SampleDTO sampleDTO){
    var toAdd = ProxySolarSample.builder()
        .sample(ProxySampleDTO.builder()
            .systemId(systemId)
            .sampleDTO(sampleDTO)
            .build())
        .build();
    proxySolarSampleRepository.save(toAdd);
  }


}
