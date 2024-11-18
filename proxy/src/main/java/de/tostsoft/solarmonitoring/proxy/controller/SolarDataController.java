package de.tostsoft.solarmonitoring.proxy.controller;

import de.tostsoft.solarmonitoring.lib.controller.BaseSolarDataController;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.service.SolarDataValidator;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.service.ProxySolarSystemService;
import de.tostsoft.solarmonitoring.proxy.service.SolarDataService;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SolarDataController extends BaseSolarDataController {

  @Autowired
  private SolarDataValidator solarDataValidator;

  @Autowired
  private ProxySolarSystemService proxySolarSystemService;

  @Autowired
  private SolarDataService solarDataService;

  @Value("${api.tokens.deye:}")
  private String deyeEndpointSunApiToken;

  @Value("${proxy.timeout:86400000}")//3 days
  private Long systemTimeout;

  private void checkSystemUpToDate(ProxySolarSystem system){
    if(system.getLastUpdate() + systemTimeout < System.currentTimeMillis()){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System is not up to date");
    }
  }

  public void PostDevice( String systemId, SampleDTO solarSample, String clientToken) {
    var sys = proxySolarSystemService.findMatchingSystemWithToken(systemId, clientToken);//throws exception if not found
    checkSystemUpToDate(sys);
    solarDataValidator.validateAndFillMissing(solarSample);
    solarDataService.addSolarSample(systemId,solarSample);
  }

  @Override
  public void PostDeviceMult(String systemId, List<SampleDTO> solarSamples, String clientToken) {
    var sys = proxySolarSystemService.findMatchingSystemWithToken(systemId, clientToken);//throws exception if not found
    checkSystemUpToDate(sys);
    for (SampleDTO solarSample : solarSamples) {
      solarDataValidator.validateAndFillMissing(solarSample);
    }
    for (SampleDTO solarSample : solarSamples) {
      solarDataService.addSolarSample(systemId,solarSample);
    }
  }

  @Override
  public void PostDeviceDeye(String serialId, SampleDTO solarSample, String clientToken) {

    if(StringUtils.isEmpty(deyeEndpointSunApiToken)){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not activated");
    }

    if(!StringUtils.equals(deyeEndpointSunApiToken,clientToken)){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This Endpoint requires Authentication");
    }

    long serial;

    try{
      serial = Long.parseLong(serialId);
    }catch (NumberFormatException exception){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serialId must be numeric");
    }

    var sys = proxySolarSystemService.findMatchingSystemWithDeyeSunSerial(serial);//throws exception if not found
    checkSystemUpToDate(sys);

    solarDataValidator.validateAndFillMissing(solarSample);

    solarDataService.addSolarSample(sys.getId(),solarSample);
  }
}
