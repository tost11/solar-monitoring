package de.tostsoft.solarmonitoring.app.controller;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericSolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarDeviceInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.app.service.SolarService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static de.tostsoft.solarmonitoring.app.controller.SolarController.*;

//TODO reorder class this here is more of the solar service than the original solar service
@Service
public class SolarDataConverter {

  private static final Logger LOG = LoggerFactory.getLogger(SolarDataConverter.class);

  @Autowired
  private SolarService solarService;
  @Autowired
  private SolarSystemRepository solarSystemRepository;
  @Autowired
  private InfluxService influxService;

  static public void setGenericInfluxPointBaseClassAttributes(GenericInfluxPoint influxPoint, float duration, Long timestamp, String systemId){
    influxPoint.setTimestamp(timestamp);
    influxPoint.setDuration(duration);
    influxPoint.setSystemId(systemId);
  }

  public interface ValidateAndConvertInterface<T>{
    GenericInfluxPoint validateAndConvert(T solarSample);
  }

  public interface MultiValidateAndConvertInterface<T>{
    List<GenericInfluxPoint> validateAndConvert(T solarSample,SolarSystem solarSystem);
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
      /*var now = Instant.now();
      var last = Instant.ofEpochMilli(lastPoint.getTimestamp());
      if(Duration.between(now,last).toMinutes() > 3){//when failewise to large values are written
        return;
      }*/

      if(lastPoint.getInputWatt() == null && lastPoint.getBatteryWatt() == null){
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
    var influxPoint = validateAndConvertInterface.validateAndConvert(solarSample,system);
    var last = solarService.addSolarData(system,influxPoint);

    if(system.getCalculateTotalValuesAfterwards()){
      last =  generateSumPoint(system,influxPoint);
    }

    updateMongo(system, last);
  }

  public <T> void genericHandleMultipleMulti(String systemId, List<T> solarSamples, String clientToken, MultiValidateAndConvertInterface<T> validateAndConvertInterface){
    var system = solarService.findMatchingSystemWithToken(systemId,clientToken);
    List<GenericInfluxPoint> influxPoints = new ArrayList<>(solarSamples.size());
    for (var solarSample : solarSamples) {
      var points = validateAndConvertInterface.validateAndConvert(solarSample,system);
      influxPoints.addAll(points);
    }
    var last = solarService.addSolarData(system,influxPoints);

    if(system.getCalculateTotalValuesAfterwards()){
      last = generateSumPoint(system,influxPoints);
    }

    updateMongo(system,last);
  }

  public <T> void genericHandleDeye(Long serial,T solarSample,DeyeValidateAndConvertInterface<T> validateAndConvertInterface){

    var system = solarService.findMatchingSystemWithDeyeSunSerial(serial);
    var influxPoint = validateAndConvertInterface.validateAndConvert(system,solarSample).stream().filter(f->f.getMeasurement() != InfluxMeasurement.SOLAR_DATA).toList();
    var last = solarService.addSolarData(system,influxPoint);

    //if(system.getCalculateTotalValuesAfterwards()){
      last = generateSumPoint(system,influxPoint);
    //}

    updateMongo(system,last);
  }

  private void setValueByReflection(SolarDeviceInfluxPoint device,String methodName,Number value){
    try {
      Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, Float.class);
      testMethod.invoke(device, value.floatValue());
      return;
    }catch (Exception ex){
      LOG.debug("Error while calling method via reflection",ex);
    }
    try {
      Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, float.class);
      testMethod.invoke(device, value.floatValue());
      return;
    }catch (Exception ex){
      LOG.debug("Error while calling method via reflection",ex);
    }
    try {
      Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, Integer.class);
      testMethod.invoke(device, value.intValue());
      return;
    }catch (Exception ex){
      LOG.debug("Error while calling method via reflection",ex);
    }
    try {
      Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, int.class);
      testMethod.invoke(device, value.intValue());
    }catch (Exception ex){
      LOG.debug("Error while calling method via reflection",ex);
    }
  }

  private SolarInfluxPoint generateSumPoint(SolarSystem system,List<GenericInfluxPoint> influxPoints){
    var stamps = new HashMap<Long,Float>();
    for (GenericInfluxPoint influxPoint : influxPoints) {
      stamps.put(influxPoint.getTimestamp(),influxPoint.getDuration());
    }

    var resPoints = new ArrayList<GenericInfluxPoint>();
    for (var stamp : stamps.entrySet()) {
      var points = influxService.getDevicePointsInTimeRange(system, Instant.ofEpochMilli(stamp.getKey()));
      var convertedPoints = readableDeviceInfluxPoints(points);
      var point = combineDeviceInfluxPoints(convertedPoints,stamp.getValue(),stamp.getKey(),system.getInfluxTagName());
      resPoints.add(point);
    }
    return solarService.addSolarData(system,resPoints);
  }

  private SolarInfluxPoint combineDeviceInfluxPoints(List<SolarDeviceInfluxPoint> devicePoints, float duration,Long timestamp,String systemId){

    //this is mostly just a copy of converter function in conroller

    var influxPoint = SolarInfluxPoint.builder().build();

    influxPoint.setInputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattDC).collect(Collectors.toList())));
    influxPoint.setInputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getInputVoltageDC(),d.getInputWattDC())).collect(Collectors.toList()),influxPoint.getInputWattDC()));
    if(influxPoint.getInputWattDC() != null && influxPoint.getInputVoltageDC() != null) {
      influxPoint.setInputAmpereDC(influxPoint.getInputVoltageDC() <= 0 ? 0 : influxPoint.getInputWattDC() / influxPoint.getInputVoltageDC());
    }

    influxPoint.setInputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattAC).collect(Collectors.toList())));
    influxPoint.setInputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getInputVoltageAC(),d.getInputWattAC())).collect(Collectors.toList()),influxPoint.getInputWattAC()));
    if(influxPoint.getInputWattAC() != null && influxPoint.getInputVoltageAC() != null) {
      influxPoint.setInputAmpereAC(influxPoint.getInputVoltageAC() <= 0 ? 0 : influxPoint.getInputWattAC() / influxPoint.getInputVoltageAC());
    }

    influxPoint.setBatteryWatt(calculateSum(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryWatt).collect(Collectors.toList())));
    influxPoint.setBatteryVoltage(calculateMean(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryVoltage).collect(Collectors.toList())));
    if(influxPoint.getBatteryWatt() != null && influxPoint.getBatteryVoltage() != null) {
      influxPoint.setBatteryAmpere(influxPoint.getBatteryWatt() / influxPoint.getBatteryVoltage());
    }

    influxPoint.setOutputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattDC).collect(Collectors.toList())));
    influxPoint.setOutputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltageDC(),d.getOutputWattDC())).collect(Collectors.toList()),influxPoint.getOutputWattDC()));
    if(influxPoint.getOutputWattDC() != null && influxPoint.getOutputVoltageDC() != null) {
      influxPoint.setOutputAmpereDC(influxPoint.getOutputVoltageDC() <= 0 ? 0 : influxPoint.getOutputWattDC() / influxPoint.getOutputVoltageDC());
    }

    influxPoint.setOutputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattAC).collect(Collectors.toList())));
    influxPoint.setOutputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltageAC(),d.getOutputWattAC())).collect(Collectors.toList()),influxPoint.getOutputWattAC()));
    if(influxPoint.getOutputWattAC() != null && influxPoint.getOutputVoltageAC() != null) {
      influxPoint.setOutputAmpereAC(influxPoint.getOutputVoltageAC() <= 0 ? 0 : influxPoint.getOutputWattAC() / influxPoint.getOutputVoltageAC());
    }

    influxPoint.setInputWatt(calculateSum(Arrays.asList(influxPoint.getInputWattDC(),influxPoint.getInputWattAC())));
    influxPoint.setOutputWatt(calculateSum(Arrays.asList(influxPoint.getOutputWattDC(),influxPoint.getOutputWattAC())));

    influxPoint.setInputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getInputFrequency).filter(
            Objects::nonNull).collect(Collectors.toList())));

    influxPoint.setOutputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputFrequency).filter(
            Objects::nonNull).collect(Collectors.toList())));

    influxPoint.setBatteryPercentage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryPercentage).filter(
            Objects::nonNull).collect(Collectors.toList())));

    influxPoint.setTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTemperature).filter(
            Objects::nonNull).collect(Collectors.toList())));

    influxPoint.setBatteryTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryTemperature).filter(
            Objects::nonNull).collect(Collectors.toList())));

    if(influxPoint.getTotalOH() == null){
      influxPoint.setTotalOH(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTotalOH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    //TODO think about total values and duration stuff and implement that within thinking of different durations

    setGenericInfluxPointBaseClassAttributes(influxPoint,duration,timestamp,systemId);

    return influxPoint;
  }

  private List<SolarDeviceInfluxPoint> readableDeviceInfluxPoints(List<FluxTable> fluxTables){

    Map<Instant, Map<Long, SolarDeviceInfluxPoint>> resMap = new HashMap<>();

    List<DeviceDTO> devices = new ArrayList<>();

    List<SolarDeviceInfluxPoint> influxPoints = new ArrayList<>();

    for(var fluxTable : fluxTables){
      for(var record : fluxTable.getRecords()){

        Number number = (Number) record.getValueByKey("_value");
        if(number == null){
          continue;
        }

        var time = ((Instant) record.getValueByKey("_time"));
        if(!resMap.containsKey(time)){
          resMap.put(time,new HashMap<>());
        }
        var res = resMap.get(time);
        Long id = Long.parseLong(""+record.getValueByKey("id"));

        if(!res.containsKey(id)){
          res.put(id,SolarDeviceInfluxPoint.builder().id(id).build());
        }
        var deviceDTO = res.get(id);

          String name = record.getField();
          if(StringUtils.length(name) < 1){
            continue;
          }
          name = name.substring(0, 1).toUpperCase() + name.substring(1);
          name = "set" + name;
          System.out.println(name);
          setValueByReflection(deviceDTO,name,number);
      }
    }

    for (Map<Long, SolarDeviceInfluxPoint> value : resMap.values()) {
      for (SolarDeviceInfluxPoint solarDeviceInfluxPoint : value.values()) {
        if(solarDeviceInfluxPoint.getDuration() <= 0){
          continue;
        }
        influxPoints.add(solarDeviceInfluxPoint);
      }
    }
    return influxPoints;
  }
}
