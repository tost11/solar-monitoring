package de.tostsoft.solarmonitoring.controller.data;

import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.service.SolarService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolarDataConverter {

  @Autowired
  private SolarService solarService;
  @Autowired
  private InfluxConnection influxConnection;

  static public void setGenericInfluxPointBaseClassAttributes(GenericInfluxPoint influxPoint,float duration,Long timestamp,long systemId){
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


  public <T> void genericHandleMulti(long systemId,T solarSample,String clientToken,SolarSystemType type,MultiValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    if(system.getType() != type){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This is not the correct enpoint for this sort of solar system");
    }
    var influxPoint = validateAndConvertInterface.validateAndConvert(solarSample);
    influxPoint.forEach(p->p.setType(type));
    solarService.addSolarData(system,influxPoint);
  }

  public <T> void genericHandleMultipleMulti(long systemId, List<T> solarSamples,String clientToken,SolarSystemType type,MultiValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    if(system.getType() != type){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This is not the correct enpoint for this sort of solar system");
    }
    List<GenericInfluxPoint> influxPoints = new ArrayList<>(solarSamples.size());
    for (var solarSample : solarSamples) {
      var points = validateAndConvertInterface.validateAndConvert(solarSample);
      points.forEach(p->p.setType(type));
      influxPoints.addAll(points);
    }
    solarService.addSolarData(system,influxPoints);
  }


}
