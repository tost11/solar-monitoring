package de.tostsoft.solarmonitoring.proxy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.lib.controller.BaseSolarDataController;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.service.SolarDataValidator;
import de.tostsoft.solarmonitoring.proxy.dtos.MultDataResponseProxyDTO;
import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import de.tostsoft.solarmonitoring.proxy.service.ProxySolarSystemService;
import de.tostsoft.solarmonitoring.proxy.service.SolarDataService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
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

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private Validator validator;

  public static final int MAX_MULT_REQUEST_SAMPLES_SIZE = 30;

  private void checkSystemUpToDate(ProxySolarSystem system){
    if(system.getLastUpdate() + systemTimeout < System.currentTimeMillis()){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System is not up to date");
    }
  }

  public void PostDevice( String systemId, SampleDTO solarSample, String clientToken) {
    var sys = proxySolarSystemService.findMatchingSystemWithToken(systemId, clientToken);//throws exception if not found
    checkSystemUpToDate(sys);
    solarDataValidator.validateAndFillMissing(solarSample);
    solarDataService.addSolarSample(systemId, Collections.singletonList(solarSample));
  }

  @Override
  public ResponseEntity<String> PostDeviceMult(String systemId, @NotNull List<SampleDTO> solarSamples, String clientToken) {

    if(solarSamples.size() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Samples list is empty");
    }

    if(solarSamples.size() > MAX_MULT_REQUEST_SAMPLES_SIZE){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"To many samples for mult request, max is "+MAX_MULT_REQUEST_SAMPLES_SIZE);
    }

    var sys = proxySolarSystemService.findMatchingSystemWithToken(systemId, clientToken);//throws exception if not found

    MultDataResponseProxyDTO res =  new MultDataResponseProxyDTO();

    List<SampleDTO> fineSamples = new ArrayList<>();

    checkSystemUpToDate(sys);
    for (int i = 0;i<solarSamples.size();i++) {
        var solarSample = solarSamples.get(i);
        try{
            var errors = validator.validate(solarSample);
            if(!errors.isEmpty()){
                throw new RuntimeException("Validation failed");
            }
            solarDataValidator.validateAndFillMissing(solarSample);
            fineSamples.add(solarSample);
        }catch(Exception e){
            res.getInvalidSamplesIndexes().add(i);
            continue;
        }
        solarDataService.addSolarSample(systemId,fineSamples);
    }

    var status = res.getInvalidSamplesIndexes().size() != solarSamples.size() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    try {
      return new ResponseEntity<>(objectMapper.writeValueAsString(res),status);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);//this can not happen
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

    solarDataService.addSolarSample(sys.getId(),Collections.singletonList(solarSample));
  }
}
