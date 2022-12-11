package de.tostsoft.solarmonitoring.controller;

import static de.tostsoft.solarmonitoring.controller.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.InputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.OutputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.model.influx.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
@RequestMapping("/api/solar/data/grid")
public class SolarController {

  @Autowired
  private SolarDataConverter solarDataConverter;

  private void validateAndFillMissing(InputDTO solarSample){
    if(solarSample.getId() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
    }
    if(solarSample.getVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Input Voltage must be above or zero");
    }
    if(solarSample.getAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Input Ampere must be above or zero");
    }
    if (solarSample.getWatt() == null) {
      solarSample.setWatt(solarSample.getAmpere() * solarSample.getVoltage());
    }else if(solarSample.getWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Input Watt must be above or zero");
    }
  }

  private void validateAndFillMissing(OutputDTO solarSample){
    if(solarSample.getId() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
    }
    if(solarSample.getVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Output Voltage must be above zero");
    }
    if(solarSample.getAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Output Ampere must be above or zero");
    }
    if (solarSample.getWatt() == null) {
      solarSample.setWatt(solarSample.getAmpere() * solarSample.getVoltage());
    }else if(solarSample.getWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Output Watt must be above or zero");
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
    if(values.isEmpty()){
      return null;
    }
    float res = 0;
    for (var value : values) {
      res += value.getLeft() * value.getRight() / max;
    }
    return res;
  }

  private Float calculateSum(List<Float> values){
    if(values.isEmpty()){
      return null;
    }
    float res = 0;;
    for (Float value : values) {
      res += value;
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


  private void validateSolarSampleDTO(final SampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }
    if(CollectionUtils.isEmpty(solarSample.getDevices())){
      solarSample.setDevices(new ArrayList<>());
    }
    if(solarSample.getInputVoltage() == null){
      if(solarSample.getInputAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getInputVoltage() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage must be above or zero");
      }
      if(solarSample.getInputAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(solarSample.getInputAmpere() == null){
      if(solarSample.getInputVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getInputAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeAmpere must be above or zero");
      }
      if(solarSample.getInputVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }

    if(solarSample.getOutputVoltage() == null){
      if(solarSample.getOutputAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getOutputVoltage() <= 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage must be above zero");
      }
      if(solarSample.getOutputAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(solarSample.getOutputAmpere() == null){
      if(solarSample.getOutputVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getOutputAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridAmpere must be above or zero");
      }
      if(solarSample.getOutputVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
  }

  private void validateGridDeviceDTO(DeviceDTO device){

    if(device.getId() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
    }

    if(device.getInputVoltage() == null){
      if(device.getInputAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getInputVoltage() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage must be above or zero");
      }
      if(device.getInputAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(device.getInputAmpere() == null){
      if(device.getInputVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getInputAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeAmpere must be above or zero");
      }
      if(device.getInputVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }

    if(device.getOutputVoltage() == null){
      if(device.getOutputAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getOutputVoltage() <= 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage must be above zero");
      }
      if(device.getOutputAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(device.getOutputAmpere() == null){
      if(device.getOutputVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getOutputAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridAmpere must be above or zero");
      }
      if(device.getOutputVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
  }

  private void validateAndFillMissing(SampleDTO solarSample){
    validateSolarSampleDTO(solarSample);
    
    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }

    if (solarSample.getInputWatt() == null && solarSample.getInputAmpere() != null && solarSample.getInputVoltage() != null) {
      solarSample.setInputWatt(solarSample.getInputAmpere() * solarSample.getInputVoltage());
    }
    if (solarSample.getOutputWatt() == null && solarSample.getOutputAmpere() != null && solarSample.getOutputVoltage() != null) {
      solarSample.setOutputWatt(solarSample.getOutputAmpere() * solarSample.getOutputVoltage());
    }


    for (var device : solarSample.getDevices()) {

      validateGridDeviceDTO(device);

      if (device.getInputWatt() == null && device.getInputAmpere() != null && device.getInputVoltage() != null) {
        device.setInputWatt(device.getInputAmpere() * device.getInputVoltage());
      }
      if (device.getOutputWatt() == null && device.getOutputAmpere() != null && device.getOutputVoltage() != null) {
        device.setOutputWatt(device.getOutputAmpere() * device.getOutputVoltage());
      }

      if(device.getInputs() == null){
        device.setInputs(new ArrayList<>());
      }
      if(device.getOutputs() == null){
        device.setOutputs(new ArrayList<>());
      }

      device.getInputs().forEach(this::validateAndFillMissing);
      device.getOutputs().forEach(this::validateAndFillMissing);
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
        if(devicePoint.getInputVoltage() <= 0){
          devicePoint.setInputAmpere(0.f);
        }else{
          devicePoint.setInputAmpere(devicePoint.getInputWatt()/devicePoint.getInputVoltage());
        }
      }else{
        devicePoint.setInputWatt(device.getInputWatt());
        devicePoint.setInputAmpere(device.getInputAmpere());
        devicePoint.setInputVoltage(device.getInputVoltage());
      }

      if(device.getOutputWatt() == null){
        devicePoint.setOutputWatt(calculateSum(deviceOutputWatts));
        devicePoint.setOutputVoltage(calculateMeanByPercentage(device.getOutputs().stream().map(o->new ImmutablePair<Float,Float>(o.getVoltage(),o.getWatt())).collect(Collectors.toList()),devicePoint.getOutputWatt()));
        devicePoint.setOutputAmpere(devicePoint.getOutputWatt()/devicePoint.getOutputVoltage());
      }else{
        devicePoint.setOutputWatt(device.getOutputWatt());
        devicePoint.setOutputAmpere(device.getOutputAmpere());
        devicePoint.setOutputVoltage(device.getOutputVoltage());
      }

      if(devicePoint.getInputWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate ChargeWatt -> missing 'charge parameters'");
      }
      if(devicePoint.getOutputWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate GridWatt -> missing 'grid parameters'");
      }

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

  @PostMapping("/")
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
