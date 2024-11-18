package de.tostsoft.solarmonitoring.whatever.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.whatever.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.whatever.repository.ProxySolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProxySolarSystemService {

  @Autowired
  private ProxySolarSystemRepository proxySolarSystemRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public ProxySolarSystem findMatchingSystemWithToken(String systemId, String token){
    var systemOpt = proxySolarSystemRepository.findById(systemId);
    if(systemOpt.isEmpty()){
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System with this id dose not exist");
    }
    var system = systemOpt.get();
    if(!passwordEncoder.matches(token,system.getToken())){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return system;
  }

  public ProxySolarSystem findMatchingSystemWithDeyeSunSerial(Long serial){
    var systemOpt = proxySolarSystemRepository.findSolarSystemBySerialInAndDeyeSunSerials(serial);
    if(systemOpt.isEmpty()){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return systemOpt.get();
  }


}
