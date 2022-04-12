package de.tostsoft.solarmonitoring.controller.data;

import static de.tostsoft.solarmonitoring.controller.data.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.simple.SimpleSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.simple.VerySimpleSampleDTO;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarInfluxPoint;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/solar/data/simple")
public class SimpleSolarController {

  @Autowired
  private SolarDataConverter solarDataConverter;

  private void validateAndFillMissing(SimpleSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }
    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }
    if (solarSample.getWatt() == null) {
      solarSample.setWatt(solarSample.getVoltage() * solarSample.getAmpere());
    }
  }

  private SelfMadeSolarInfluxPoint convertToInfluxPoint(SimpleSampleDTO solarSample,long systemId){
    var influxPoint = SelfMadeSolarInfluxPoint.builder()
        .chargeVolt(solarSample.getVoltage())
        .chargeAmpere(solarSample.getAmpere())
        .chargeWatt(solarSample.getWatt())
        .deviceTemperature(solarSample.getDeviceTemperature())
        .build();

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    return influxPoint;
  }

  @PostMapping()
  public void PostDataSimple(@RequestParam long systemId, @RequestBody @Valid SimpleSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.SIMPLE,(SimpleSampleDTO sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/mult")
  public void PostDataSimpleMult(@RequestParam long systemId, @RequestBody @Valid List<SimpleSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.SIMPLE,(SimpleSampleDTO sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  // ------------------------------------------------------ very simple ------------------------------------------------


  private void validateAndFillMissing(VerySimpleSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }
    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }
  }

  private SelfMadeSolarInfluxPoint convertToInfluxPoint(VerySimpleSampleDTO solarSample,long systemId){
    var influxPoint = SelfMadeSolarInfluxPoint.builder()
        .chargeWatt(solarSample.getWatt())
        .deviceTemperature(solarSample.getDeviceTemperature())
        .build();

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    return influxPoint;
  }


  @PostMapping("/watt")
  public void PostDataVerySimple(@RequestParam long systemId, @RequestBody @Valid VerySimpleSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.VERY_SIMPLE,(VerySimpleSampleDTO sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/watt/mult")
  public void PostDataVerySimpleMult(@RequestParam long systemId, @RequestBody @Valid List<VerySimpleSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.VERY_SIMPLE,(VerySimpleSampleDTO sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }
}
