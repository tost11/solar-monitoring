package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.app.service.SolarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolarDataConverter {

  @Autowired
  private SolarService solarService;
  @Autowired
  private SolarSystemRepository solarSystemRepository;

  static public void setGenericInfluxPointBaseClassAttributes(GenericInfluxPoint influxPoint, float duration, Long timestamp, String systemId){
    influxPoint.setTimestamp(timestamp);
    influxPoint.setDuration(duration);
    influxPoint.setSystemId(systemId);
  }

  public interface ValidateAndConvertInterface<T>{
    GenericInfluxPoint validateAndConvert(T solarSample);
  }

  public interface MultiValidateAndConvertInterface<T>{
    List<GenericInfluxPoint> validateAndConvert(T solarSample);
  }


  public interface DeyeValidateAndConvertInterface<T>{
    List<GenericInfluxPoint> validateAndConvert(SolarSystem solarSystem,T solarSample);
  }


  /*
  public <T> void genericHandle(long systemId,T solarSample,String clientToken,SolarSystemType type,ValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    if(system.getType() != type){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This is not the correct enpoint for this sort of solar system");
    }
    var influxPoint = validateAndConvertInterface.validateAndConvert(solarSample);
    influxPoint.setType(type);
    solarService.addSolarData(system,influxPoint);
  }

  public <T> void genericHandleMultiple(long systemId, List<T> solarSamples,String clientToken,SolarSystemType type,ValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    if(system.getType() != type){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This is not the correct enpoint for this sort of solar system");
    }
    List<GenericInfluxPoint> influxPoints = new ArrayList<>(solarSamples.size());
    for (var solarSample : solarSamples) {
      var point = validateAndConvertInterface.validateAndConvert(solarSample);
      point.setType(type);
      influxPoints.add(point);
    }
    solarService.addSolarData(system,influxPoints);
  }
*/

  void updateMongo(SolarSystem system, SolarInfluxPoint lastPoint){
    solarSystemRepository.updateNeedsStatisticRecalculation(system.getId(),true);
    if(lastPoint != null) {
      var now = Instant.now();
      var last = Instant.ofEpochMilli(lastPoint.getTimestamp());
      if(Duration.between(now,last).toMinutes() > 3){//when failewise to large values are written
        return;
      }
      solarSystemRepository.updateCurrentValuesIfNewer(system.getId(), lastPoint.getTimestamp(),
        CurrentValues.builder()
                .lastSet(lastPoint.getTimestamp())
                .inputWatt(lastPoint.getInputWatt())
                .batteryVoltage(lastPoint.getBatteryVoltage())
                .build());
    }
  }

  public <T> void genericHandleMulti(String systemId,T solarSample,String clientToken,MultiValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    var influxPoint = validateAndConvertInterface.validateAndConvert(solarSample);
    var last = solarService.addSolarData(system,influxPoint);
    updateMongo(system,last);
  }

  public <T> void genericHandleMultipleMulti(String systemId, List<T> solarSamples, String clientToken, MultiValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    List<GenericInfluxPoint> influxPoints = new ArrayList<>(solarSamples.size());
    for (var solarSample : solarSamples) {
      var points = validateAndConvertInterface.validateAndConvert(solarSample);
      influxPoints.addAll(points);
    }
    var last = solarService.addSolarData(system,influxPoints);
    updateMongo(system,last);
  }

  public <T> void genericHandleDeye(Long serial,T solarSample,DeyeValidateAndConvertInterface<T> validateAndConvertInterface){

    var system = solarService.findMatchingSystemWithDeyeSunSerial(serial);
    var influxPoint = validateAndConvertInterface.validateAndConvert(system,solarSample);
    var last = solarService.addSolarData(system,influxPoint);
    updateMongo(system,last);
  }


}
