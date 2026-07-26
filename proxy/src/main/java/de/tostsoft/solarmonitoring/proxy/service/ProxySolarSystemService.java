package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ProxySolarSystemService {

  @Autowired
  private ProxySolarSystemRepository proxySolarSystemRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public ProxySolarSystem findSystemById(String systemId) {
    return proxySolarSystemRepository.findById(systemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Authentication failed: wrong credentials or system does not exist"));
  }

  public ProxySolarSystem findMatchingSystemWithToken(String systemId, String token){
    var systemOpt = proxySolarSystemRepository.findById(systemId);
    if(systemOpt.isEmpty()){
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: wrong credentials or system does not exist");
    }
    var system = systemOpt.get();

    if (!CollectionUtils.isEmpty(system.getTokens())) {
        for (AccessToken accessToken : system.getTokens()) {
            if (accessToken.getPurpose() != TokenPurpose.DATA_PUSH_REST) {
                continue;
            }
            if (accessToken.getExpiresAt() != null && accessToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                continue;
            }
            if (passwordEncoder.matches(token, accessToken.getHash())) {
                return system;
            }
        }
    }

    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: wrong credentials or system does not exist");
  }

  public ProxySolarSystem findMatchingSystemWithDeyeSunSerial(Long serial){
    var systemOpt = proxySolarSystemRepository.findSolarSystemBySerialInAndDeyeSunSerials(serial);
    if(systemOpt.isEmpty()){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"System with this serial not found");
    }
    return systemOpt.get();
  }


}
