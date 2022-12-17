package de.tostsoft.solarmonitoring.controller;

import static de.tostsoft.solarmonitoring.controller.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.*;
import de.tostsoft.solarmonitoring.model.influx.*;

import java.util.*;
import java.util.stream.Collectors;
import javax.validation.Valid;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/solar/data")
public class SolarController {

  @Autowired
  private SolarDataConverter solarDataConverter;

  private void validateAndFillMissing(InputDTO sample){
    //for watt
    if (sample.getWatt() == null && sample.getAmpere() != null && sample.getVoltage() != null) {
      sample.setWatt(sample.getAmpere() * sample.getVoltage());
    }

    //for ampere
    if (sample.getAmpere() == null && sample.getWatt() != null && sample.getVoltage() != null) {
      sample.setAmpere(sample.getVoltage() == 0 ? 0 : sample.getWatt() / sample.getVoltage());
    }

    //for voltage
    if (sample.getVoltage() == null && sample.getWatt() != null && sample.getAmpere() != null) {
      sample.setVoltage(sample.getAmpere() == 0 ? null : sample.getWatt() / sample.getAmpere());
    }
  }

  private void validateAndFillMissing(OutputDTO sample){
    //for watt
    if (sample.getWatt() == null && sample.getAmpere() != null && sample.getVoltage() != null) {
      sample.setWatt(sample.getAmpere() * sample.getVoltage());
    }

    //for ampere
    if (sample.getAmpere() == null && sample.getWatt() != null && sample.getVoltage() != null) {
      sample.setAmpere(sample.getVoltage() == 0 ? 0 : sample.getWatt() / sample.getVoltage());
    }

    //for voltage
    if (sample.getVoltage() == null && sample.getWatt() != null && sample.getAmpere() != null) {
      sample.setVoltage(sample.getAmpere() == 0 ? null : sample.getWatt() / sample.getAmpere());
    }
  }

  private void validateAndFillMissing(BatteryDTO sample){

    if(sample.getAmpere() != null && sample.getWatt() != null){
      validateThrow((sample.getAmpere() > 0 && sample.getWatt() < 0) ||
              (sample.getAmpere() < 0 && sample.getWatt() > 0)
              ,"Watt and Ampere on BatterySample "+sample.getId()+" are not both positive or negative");
    }

    //for watt
    if (sample.getWatt() == null && sample.getAmpere() != null && sample.getVoltage() != null) {
      sample.setWatt(sample.getAmpere() * sample.getVoltage());
    }

    //for ampere
    if (sample.getAmpere() == null && sample.getWatt() != null && sample.getVoltage() != null) {
      sample.setAmpere(sample.getVoltage() == 0 ? 0 : sample.getWatt() / sample.getVoltage());
    }

    //for voltage
    if (sample.getVoltage() == null && sample.getWatt() != null && sample.getAmpere() != null) {
      sample.setVoltage(sample.getAmpere() == 0 ? null : sample.getWatt() / sample.getAmpere());
      if(sample.getVoltage() != null && sample.getVoltage() < 0){
        sample.setVoltage(sample.getVoltage()*-1);
      }
    }
  }

  private SolarInInputInfluxPoint convertInputDTO(InputDTO solarSample, Long deviceId){
    return SolarInInputInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarOutputInfluxPoint convertOutputDTO(OutputDTO solarSample, Long deviceId){
    return SolarOutputInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .frequency(solarSample.getFrequency())
        .phase(solarSample.getPhase())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private Float calculateMean(List<Float> values){
    if(values.isEmpty()){
      return null;
    }
    float res = 0;
    float mult = 1.f / values.size();
    for (Float value : values) {
      res += value * mult;
    }
    return res;
  }

  private Float calculateMeanByPercentage(List<Pair<Float,Float>> values,Float max){
    if(max == null){
      return null;
    }
    if(values.isEmpty()){
      return null;
    }
    Float res = null;
    for (var value : values) {
      if(value.getLeft() == null || value.getRight() == null){
        continue;
      }
      float v = value.getLeft() * value.getRight() / max;
      if(res == null){
        res = v;
      }else{
        res += v;
      }
    }
    return res;
  }

  private Float calculateSum(List<Float> values){
    if(values.isEmpty()){
      return null;
    }
    Float res = null;
    for (Float value : values) {
      if(value == null){
        continue;
      }
      if(res == null){
        res = value;
      }else {
        res += value;
      }
    }
    return res;
  }

  private Float addWithZeroCheck(Float old,Float toAdd){
    if(toAdd == null){
      return old;
    }
    if(old == null){
      return toAdd;
    }
    return old+toAdd;
  }

  // ---------------------------------------------------- device ------------------------------------------------------


  private void validateThrow(boolean value,String message){
    if(value){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  private void validateSolarSampleDTO(final SampleDTO solarSample){
    if(CollectionUtils.isEmpty(solarSample.getDevices())){
      solarSample.setDevices(new ArrayList<>());
    }
  }

  private void validateDeviceDTO(DeviceDTO device){

    if(device.getBatteryAmpere() != null && device.getBatteryWatt() != null) {
      validateThrow((device.getBatteryAmpere() > 0 && device.getBatteryWatt() < 0) ||
                      (device.getBatteryAmpere() < 0 && device.getBatteryWatt() > 0)
              , "Watt and Ampere on device " + device.getId() + " are not both positive or negative");
    }

    //for watt
    if (device.getInputWatt() == null && device.getInputAmpere() != null && device.getInputVoltage() != null) {
      device.setInputWatt(device.getInputAmpere() * device.getInputVoltage());
    }
    if (device.getOutputWatt() == null && device.getOutputAmpere() != null && device.getOutputVoltage() != null) {
      device.setOutputWatt(device.getOutputAmpere() * device.getOutputVoltage());
    }
    if (device.getBatteryWatt() == null && device.getBatteryAmpere() != null && device.getBatteryVoltage() != null) {
      device.setBatteryWatt(device.getBatteryAmpere() * device.getBatteryVoltage());
    }

    //for ampere
    if (device.getInputAmpere() == null && device.getInputWatt() != null && device.getInputVoltage() != null) {
      device.setInputAmpere(device.getInputVoltage() == 0 ? 0 : device.getInputWatt() / device.getInputVoltage());
    }
    if (device.getOutputAmpere() == null && device.getOutputWatt() != null && device.getOutputVoltage() != null) {
      device.setOutputAmpere(device.getOutputVoltage() == 0 ? 0 : device.getOutputWatt() / device.getOutputVoltage());
    }
    if (device.getBatteryAmpere() == null && device.getBatteryWatt() != null && device.getBatteryVoltage() != null) {
      device.setBatteryAmpere(device.getBatteryVoltage() == 0 ? 0 : device.getBatteryWatt() / device.getBatteryVoltage());
    }

    //for voltage
    if (device.getInputVoltage() == null && device.getInputWatt() != null && device.getInputAmpere() != null) {
      device.setInputVoltage(device.getInputAmpere() == 0 ? null : device.getInputWatt() / device.getInputAmpere());
    }
    if (device.getOutputVoltage() == null && device.getOutputWatt() != null && device.getOutputAmpere() != null) {
      device.setOutputVoltage(device.getOutputAmpere() == 0 ? null : device.getOutputWatt() / device.getOutputAmpere());
    }
    if (device.getBatteryVoltage() == null && device.getBatteryWatt() != null && device.getBatteryAmpere() != null) {
      device.setBatteryVoltage(device.getBatteryAmpere() == 0 ? null : device.getBatteryWatt() / device.getBatteryAmpere());
      if(device.getBatteryVoltage() < 0){
        device.setBatteryVoltage(device.getBatteryVoltage() * -1);
      }
    }
  }

  private void validateAndFillMissing(SampleDTO solarSample){
    validateSolarSampleDTO(solarSample);

    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }

    if(solarSample.getBatteryAmpere() != null && solarSample.getBatteryWatt() != null) {
      validateThrow((solarSample.getBatteryAmpere() > 0 && solarSample.getBatteryWatt() < 0) ||
                      (solarSample.getBatteryAmpere() < 0 && solarSample.getBatteryWatt() > 0)
              , "Watt and Ampere on solarSample are not both positive or negative");
    }

    //for watt
    if (solarSample.getInputWatt() == null && solarSample.getInputAmpere() != null && solarSample.getInputVoltage() != null) {
      solarSample.setInputWatt(solarSample.getInputAmpere() * solarSample.getInputVoltage());
    }
    if (solarSample.getOutputWatt() == null && solarSample.getOutputAmpere() != null && solarSample.getOutputVoltage() != null) {
      solarSample.setOutputWatt(solarSample.getOutputAmpere() * solarSample.getOutputVoltage());
    }
    if (solarSample.getBatteryWatt() == null && solarSample.getBatteryAmpere() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryWatt(solarSample.getBatteryAmpere() * solarSample.getBatteryVoltage());
    }

    //for ampere
    if (solarSample.getInputAmpere() == null && solarSample.getInputWatt() != null && solarSample.getInputVoltage() != null) {
      solarSample.setInputAmpere(solarSample.getInputVoltage() == 0 ? 0 : solarSample.getInputWatt() / solarSample.getInputVoltage());
    }
    if (solarSample.getOutputAmpere() == null && solarSample.getOutputWatt() != null && solarSample.getOutputVoltage() != null) {
      solarSample.setOutputVoltage(solarSample.getOutputVoltage() == 0 ? 0 : solarSample.getOutputWatt() / solarSample.getOutputVoltage());
    }
    if (solarSample.getBatteryAmpere() == null && solarSample.getBatteryWatt() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryAmpere(solarSample.getBatteryVoltage() == 0 ? 0 : solarSample.getBatteryWatt() / solarSample.getBatteryVoltage());
    }

    //for voltage
    if (solarSample.getInputVoltage() == null && solarSample.getInputWatt() != null && solarSample.getInputAmpere() != null) {
      solarSample.setInputVoltage(solarSample.getInputAmpere() == 0 ? null : solarSample.getInputWatt() / solarSample.getInputAmpere());
    }
    if (solarSample.getOutputVoltage() == null && solarSample.getOutputWatt() != null && solarSample.getOutputAmpere() != null) {
      solarSample.setOutputVoltage(solarSample.getOutputAmpere() == 0 ? null : solarSample.getOutputWatt() / solarSample.getOutputAmpere());
    }
    if (solarSample.getBatteryVoltage() == null && solarSample.getBatteryWatt() != null && solarSample.getBatteryAmpere() != null) {
      solarSample.setBatteryVoltage(solarSample.getBatteryAmpere() == 0 ? null : solarSample.getBatteryWatt() / solarSample.getBatteryAmpere());
      if(solarSample.getBatteryVoltage() < 0){
        solarSample.setBatteryVoltage(solarSample.getBatteryVoltage() * -1);
      }
    }

    var tmpDeviceIds = new HashSet<Long>();

    for (var device : solarSample.getDevices()) {

      validateThrow(tmpDeviceIds.contains(device.getId()),"Two devices with the same Id Found "+device.getId());
      tmpDeviceIds.add(device.getId());
      validateDeviceDTO(device);

      if(device.getInputs() == null){
        device.setInputs(new ArrayList<>());
      }
      if(device.getOutputs() == null){
        device.setOutputs(new ArrayList<>());
      }
      if(device.getBatteries() == null){
        device.setBatteries(new ArrayList<>());
      }

      var ids = new HashSet<Long>();
      for (var input : device.getInputs()) {
        validateThrow(ids.contains(input.getId()),"Two inputs on device "+device.getId()+" with the same Id Found "+device.getId());
        ids.add(device.getId());
        validateAndFillMissing(input);
      };

      ids.clear();
      for (var output : device.getOutputs()) {
        validateThrow(ids.contains(output.getId()),"Two outputs on device "+device.getId()+" with the same Id Found "+device.getId());
        ids.add(device.getId());
        validateAndFillMissing(output);
      };

      ids.clear();
      for (var battery : device.getBatteries()) {
        validateThrow(ids.contains(battery.getId()),"Two Batteries on device "+device.getId()+" with the same Id Found "+device.getId());
        ids.add(device.getId());
        validateAndFillMissing(battery);
      };
    }
  }

  private List<GenericInfluxPoint> convertToInfluxPoint(SampleDTO solarSample, long systemId){

    List<GenericInfluxPoint> res = new ArrayList<>();

    var influxPoint = SolarInfluxPoint.builder()
        .inputVoltage(solarSample.getInputVoltage())
        .inputAmpere(solarSample.getInputAmpere())
        .inputWatt(solarSample.getInputWatt())
        .outputVoltage(solarSample.getOutputVoltage())
        .outputAmpere(solarSample.getOutputAmpere())
        .outputWatt(solarSample.getOutputWatt())
        .inputTotalKWH(solarSample.getInputTotalKWH())
        .outputTotalKWH(solarSample.getOutputTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .inputTotalOH(solarSample.getInputTotalOH())
        .outputTotalOH(solarSample.getOutputTotalOH())
        .frequency(solarSample.getFrequency())
        .temperature(solarSample.getTemperature())
        .batteryTemperature(solarSample.getBatteryTemperature())
        .batteryVoltage(solarSample.getBatteryVoltage())
        .batteryAmpere(solarSample.getBatteryAmpere())
        .batteryWatt(solarSample.getBatteryWatt())
        .batteryPercentage(solarSample.getBatteryPercentage())
        .build();

    List<GenericSolarInfluxPoint> devicePoints = new ArrayList<>();
    Float inputTotalKWHs = null;
    Float outputTotalKWHs = null;

    for (DeviceDTO device : solarSample.getDevices()) {

      List<Float> deviceInputWatts = new ArrayList<>();
      List<Float> deviceInputVoltages = new ArrayList<>();

      List<Float> deviceOutputWatts = new ArrayList<>();
      List<Float> deviceOutputVoltages = new ArrayList<>();

      List<Float> deviceFrequencies = new ArrayList<>();

      for (InputDTO input : device.getInputs()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceInputWatts.add(input.getWatt());
        deviceInputVoltages.add(input.getVoltage());
      }

      for (OutputDTO output : device.getOutputs()) {
        var point = convertOutputDTO(output,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceOutputWatts.add(output.getWatt());
        deviceOutputVoltages.add(output.getVoltage());
        if (output.getFrequency() != null) {
          deviceFrequencies.add(output.getFrequency());
        }
      }

      var devicePoint = SolarDeviceInfluxPoint.builder()
          .inputTotalKWH(device.getInputTotalKWH())
          .outputTotalKWH(device.getOutputTotalKWH())
          .totalOH(device.getTotalOH())
          .temperature(solarSample.getTemperature())
          .id(device.getId())
          .build();

      if(device.getInputWatt() == null){
        devicePoint.setInputWatt(calculateSum(deviceInputWatts));
        devicePoint.setInputVoltage(calculateMeanByPercentage(device.getInputs().stream().map(i->new ImmutablePair<Float,Float>(i.getVoltage(),i.getWatt())).collect(Collectors.toList()),devicePoint.getInputWatt()));
        if(devicePoint.getInputWatt() != null && devicePoint.getInputVoltage() != null) {
            devicePoint.setInputAmpere(devicePoint.getInputVoltage() <= 0 ? 0 : devicePoint.getInputWatt() / devicePoint.getInputVoltage());
        }
      }else{
        devicePoint.setInputWatt(device.getInputWatt());
        devicePoint.setInputAmpere(device.getInputAmpere());
        devicePoint.setInputVoltage(device.getInputVoltage());
      }

      if(device.getOutputWatt() == null){
        devicePoint.setOutputWatt(calculateSum(deviceOutputWatts));
        devicePoint.setOutputVoltage(calculateMeanByPercentage(device.getOutputs().stream().map(o->new ImmutablePair<Float,Float>(o.getVoltage(),o.getWatt())).collect(Collectors.toList()),devicePoint.getOutputWatt()));
        if(devicePoint.getOutputWatt() != null && device.getOutputVoltage() != null) {
          devicePoint.setOutputAmpere(devicePoint.getOutputVoltage() <= 0 ? 0 : devicePoint.getOutputWatt() / devicePoint.getOutputVoltage());
        }
      }else{
        devicePoint.setOutputWatt(device.getOutputWatt());
        devicePoint.setOutputAmpere(device.getOutputAmpere());
        devicePoint.setOutputVoltage(device.getOutputVoltage());
      }

      /*if(devicePoint.getInputWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate ChargeWatt -> missing 'charge parameters'");
      }
      if(devicePoint.getOutputWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate GridWatt -> missing 'grid parameters'");
      }*/

      if(device.getFrequency() == null){
        devicePoint.setFrequency(calculateMean(deviceFrequencies));
      }else{
        devicePoint.setFrequency(calculateMean(deviceFrequencies));
      }

      setGenericInfluxPointBaseClassAttributes(devicePoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(devicePoint);

      inputTotalKWHs = addWithZeroCheck(inputTotalKWHs,device.getInputTotalKWH());
      outputTotalKWHs = addWithZeroCheck(outputTotalKWHs,device.getOutputTotalKWH());
      devicePoints.add(devicePoint);
    }

    if(influxPoint.getInputWatt() == null){
      influxPoint.setInputWatt(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWatt).collect(Collectors.toList())));
      influxPoint.setInputVoltage(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getInputVoltage(),d.getInputWatt())).collect(Collectors.toList()),influxPoint.getInputWatt()));
      if(influxPoint.getInputVoltage() <= 0){
        influxPoint.setInputAmpere(0.f);
      }else{
        influxPoint.setInputAmpere(influxPoint.getInputWatt()/influxPoint.getInputVoltage());
      }
    }

    if(influxPoint.getOutputWatt() == null){
      influxPoint.setOutputWatt(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWatt).collect(Collectors.toList())));
      influxPoint.setOutputVoltage(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltage(),d.getOutputWatt())).collect(Collectors.toList()),influxPoint.getOutputWatt()));
      influxPoint.setOutputAmpere(influxPoint.getOutputWatt()/influxPoint.getOutputVoltage());
    }

    if(influxPoint.getInputWatt() == null){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate ChargeWatt -> missing 'charge parameters'");
    }
    if(influxPoint.getOutputWatt() == null){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate GridWatt -> missing 'grid parameters'");
    }

    if(influxPoint.getFrequency() == null){
      influxPoint.setFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getFrequency).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getTemperature() == null){
      influxPoint.setTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTemperature).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getTotalOH() == null){
      influxPoint.setTotalOH(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTotalOH).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getInputTotalKWH() == null){
      influxPoint.setInputTotalKWH(inputTotalKWHs);
    }
    if(influxPoint.getOutputTotalKWH() == null){
      influxPoint.setOutputTotalKWH(outputTotalKWHs);
    }

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    res.add(influxPoint);

    return res;
  }

  @PostMapping()
  public void PostDevice(@RequestParam long systemId, @RequestBody @Valid SampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMulti(systemId,solarSample,clientToken,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/mult")
  public void PostDeviceMult(@RequestParam long systemId, @RequestBody @Valid List<SampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultipleMulti(systemId,solarSamples,clientToken,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

}
