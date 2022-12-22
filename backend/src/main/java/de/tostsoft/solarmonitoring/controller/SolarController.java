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

  private void validateAndFillMissing(InputDCDTO sample){
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

  private void validateAndFillMissing(InputACDTO sample){
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

  private void validateAndFillMissing(OutputDCDTO sample){
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

  private void validateAndFillMissing(OutputACDTO sample){
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

  private SolarInInputACInfluxPoint convertInputDTO(InputACDTO solarSample, Long deviceId){
    return SolarInInputACInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .frequency(solarSample.getFrequency())
        .phase(solarSample.getPhase())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarInInputDCInfluxPoint convertInputDTO(InputDCDTO solarSample, Long deviceId){
    return SolarInInputDCInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarOutputDCInfluxPoint convertOutputDTO(OutputDCDTO solarSample, Long deviceId){
    return SolarOutputDCInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarOutputACInfluxPoint convertOutputDTO(OutputACDTO solarSample, Long deviceId){
    return SolarOutputACInfluxPoint.builder()
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
    if (device.getInputWattDC() == null && device.getInputAmpereDC() != null && device.getInputVoltageDC() != null) {
      device.setInputWattDC(device.getInputAmpereDC() * device.getInputVoltageDC());
    }
    if (device.getOutputWattDC() == null && device.getOutputAmpereDC() != null && device.getOutputVoltageDC() != null) {
      device.setOutputWattDC(device.getOutputAmpereDC() * device.getOutputVoltageDC());
    }
    if (device.getBatteryWatt() == null && device.getBatteryAmpere() != null && device.getBatteryVoltage() != null) {
      device.setBatteryWatt(device.getBatteryAmpere() * device.getBatteryVoltage());
    }

    //for ampere
    if (device.getInputAmpereDC() == null && device.getInputWattDC() != null && device.getInputVoltageDC() != null) {
      device.setInputAmpereDC(device.getInputVoltageDC() == 0 ? 0 : device.getInputWattDC() / device.getInputVoltageDC());
    }
    if (device.getOutputAmpereDC() == null && device.getOutputWattDC() != null && device.getOutputVoltageDC() != null) {
      device.setOutputAmpereDC(device.getOutputVoltageDC() == 0 ? 0 : device.getOutputWattDC() / device.getOutputVoltageDC());
    }
    if (device.getBatteryAmpere() == null && device.getBatteryWatt() != null && device.getBatteryVoltage() != null) {
      device.setBatteryAmpere(device.getBatteryVoltage() == 0 ? 0 : device.getBatteryWatt() / device.getBatteryVoltage());
    }

    //for voltage
    if (device.getInputVoltageDC() == null && device.getInputWattDC() != null && device.getInputAmpereDC() != null) {
      device.setInputVoltageDC(device.getInputAmpereDC() == 0 ? null : device.getInputWattDC() / device.getInputAmpereDC());
    }
    if (device.getOutputVoltageDC() == null && device.getOutputWattDC() != null && device.getOutputAmpereDC() != null) {
      device.setOutputVoltageDC(device.getOutputAmpereDC() == 0 ? null : device.getOutputWattDC() / device.getOutputAmpereDC());
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
    if (solarSample.getInputWattDC() == null && solarSample.getInputAmpereDC() != null && solarSample.getInputVoltageDC() != null) {
      solarSample.setInputWattDC(solarSample.getInputAmpereDC() * solarSample.getInputVoltageDC());
    }
    if (solarSample.getOutputWattDC() == null && solarSample.getOutputAmpereDC() != null && solarSample.getOutputVoltageDC() != null) {
      solarSample.setOutputWattDC(solarSample.getOutputAmpereDC() * solarSample.getOutputVoltageDC());
    }
    if (solarSample.getBatteryWatt() == null && solarSample.getBatteryAmpere() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryWatt(solarSample.getBatteryAmpere() * solarSample.getBatteryVoltage());
    }

    //for ampere
    if (solarSample.getInputAmpereDC() == null && solarSample.getInputWattDC() != null && solarSample.getInputVoltageDC() != null) {
      solarSample.setInputAmpereDC(solarSample.getInputVoltageDC() == 0 ? 0 : solarSample.getInputWattDC() / solarSample.getInputVoltageDC());
    }
    if (solarSample.getOutputAmpereDC() == null && solarSample.getOutputWattDC() != null && solarSample.getOutputVoltageDC() != null) {
      solarSample.setOutputVoltageDC(solarSample.getOutputVoltageDC() == 0 ? 0 : solarSample.getOutputWattDC() / solarSample.getOutputVoltageDC());
    }
    if (solarSample.getBatteryAmpere() == null && solarSample.getBatteryWatt() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryAmpere(solarSample.getBatteryVoltage() == 0 ? 0 : solarSample.getBatteryWatt() / solarSample.getBatteryVoltage());
    }

    //for voltage
    if (solarSample.getInputVoltageDC() == null && solarSample.getInputWattDC() != null && solarSample.getInputAmpereDC() != null) {
      solarSample.setInputVoltageDC(solarSample.getInputAmpereDC() == 0 ? null : solarSample.getInputWattDC() / solarSample.getInputAmpereDC());
    }
    if (solarSample.getOutputVoltageDC() == null && solarSample.getOutputWattDC() != null && solarSample.getOutputAmpereDC() != null) {
      solarSample.setOutputVoltageDC(solarSample.getOutputAmpereDC() == 0 ? null : solarSample.getOutputWattDC() / solarSample.getOutputAmpereDC());
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

      if(device.getInputsDC() == null){
        device.setInputsDC(new ArrayList<>());
      }
      if(device.getOutputsDC() == null){
        device.setOutputsDC(new ArrayList<>());
      }
      if(device.getBatteries() == null){
        device.setBatteries(new ArrayList<>());
      }
      if(device.getInputsAC() == null){
        device.setInputsAC(new ArrayList<>());
      }
      if(device.getOutputsAC() == null){
        device.setOutputsAC(new ArrayList<>());
      }

      var ids = new HashSet<Long>();
      for (var input : device.getInputsDC()) {
        validateThrow(ids.contains(input.getId()),"Two dc inputs on device "+device.getId()+" with the same Id Found "+input.getId());
        ids.add(device.getId());
        validateAndFillMissing(input);
      };

      ids.clear();
      for (var output : device.getOutputsDC()) {
        validateThrow(ids.contains(output.getId()),"Two dc outputs on device "+device.getId()+" with the same Id Found "+output.getId());
        ids.add(device.getId());
        validateAndFillMissing(output);
      };

      ids.clear();
      for (var input : device.getInputsAC()) {
        validateThrow(ids.contains(input.getId()),"Two ac inputs on device "+device.getId()+" with the same Id Found "+input.getId());
        ids.add(device.getId());
        validateAndFillMissing(input);
      };

      ids.clear();
      for (var output : device.getOutputsAC()) {
        validateThrow(ids.contains(output.getId()),"Two ac outputs on device "+device.getId()+" with the same Id Found "+output.getId());
        ids.add(device.getId());
        validateAndFillMissing(output);
      };

      ids.clear();
      for (var battery : device.getBatteries()) {
        validateThrow(ids.contains(battery.getId()),"Two Batteries on device "+device.getId()+" with the same Id Found "+battery.getId());
        ids.add(device.getId());
        validateAndFillMissing(battery);
      };
    }
  }

  private List<GenericInfluxPoint> convertToInfluxPoint(SampleDTO solarSample, long systemId){

    List<GenericInfluxPoint> res = new ArrayList<>();

    var influxPoint = SolarInfluxPoint.builder()
        .inputVoltageDC(solarSample.getInputVoltageDC())
        .inputAmpereDC(solarSample.getInputAmpereDC())
        .inputWattDC(solarSample.getInputWattDC())
        .inputVoltageAC(solarSample.getInputVoltageAC())
        .inputAmpereAC(solarSample.getInputAmpereAC())
        .inputWattAC(solarSample.getInputWattAC())
        .inputWatt(solarSample.getInputWatt())
        .outputVoltageDC(solarSample.getOutputVoltageDC())
        .outputAmpereDC(solarSample.getOutputAmpereDC())
        .outputWattDC(solarSample.getOutputWattDC())
        .outputVoltageAC(solarSample.getOutputVoltageDC())
        .outputAmpereAC(solarSample.getOutputAmpereDC())
        .outputWattAC(solarSample.getOutputWattDC())
        .inputTotalKWH(solarSample.getInputTotalKWH())
        .outputTotalKWH(solarSample.getOutputTotalKWH())
        .outputWattAC(solarSample.getOutputWattAC())
        .totalOH(solarSample.getTotalOH())
        .inputTotalOH(solarSample.getInputTotalOH())
        .outputTotalOH(solarSample.getOutputTotalOH())
        .outputFrequency(solarSample.getOutputFrequency())
        .inputFrequency(solarSample.getInputFrequency())
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

      List<Float> deviceInputWattsDC = new ArrayList<>();
      List<Float> deviceInputWattsAC = new ArrayList<>();

      List<Float> deviceOutputWattsDC = new ArrayList<>();
      List<Float> deviceOutputWattsAC = new ArrayList<>();

      List<Float> inputDeviceFrequencies = new ArrayList<>();
      List<Float> outputDeviceFrequencies = new ArrayList<>();


      for (var input : device.getInputsDC()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceInputWattsDC.add(input.getWatt());
      }

      for (var input : device.getInputsAC()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceInputWattsAC.add(input.getWatt());

        if (input.getFrequency() != null) {
          inputDeviceFrequencies.add(input.getFrequency());
        }
      }

      for (var output : device.getOutputsDC()) {
        var point = convertOutputDTO(output,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceOutputWattsDC.add(output.getWatt());
      }

      for (var output : device.getOutputsAC()) {
        var point = convertOutputDTO(output,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceOutputWattsAC.add(output.getWatt());
        if (output.getFrequency() != null) {
          outputDeviceFrequencies.add(output.getFrequency());
        }
      }

      var devicePoint = SolarDeviceInfluxPoint.builder()
          .inputVoltageAC(device.getInputVoltageAC())
          .inputAmpereAC(device.getInputAmpereAC())
          .inputWattAC(device.getInputWattAC())
          .inputVoltageDC(device.getInputVoltageDC())
          .inputAmpereDC(device.getInputAmpereDC())
          .inputWattDC(device.getInputWattDC())
          .outputVoltageAC(device.getOutputVoltageAC())
          .outputAmpereAC(device.getOutputAmpereAC())
          .outputWattAC(device.getOutputWattAC())
          .outputVoltageDC(device.getOutputVoltageDC())
          .outputAmpereDC(device.getOutputAmpereDC())
          .outputWattDC(device.getOutputWattDC())
          .outputWatt(device.getOutputWatt())
          .inputTotalKWH(device.getInputTotalKWH())
          .outputTotalKWH(device.getOutputTotalKWH())
          .totalOH(device.getTotalOH())
          .temperature(solarSample.getTemperature())
          .batteryTemperature(solarSample.getBatteryTemperature())
          .inputFrequency(solarSample.getInputFrequency())
          .outputFrequency(solarSample.getOutputFrequency())
          .id(device.getId())
          .build();

      if(devicePoint.getInputWattDC() == null) {
        devicePoint.setInputWattDC(calculateSum(deviceInputWattsDC));
      }
      if(devicePoint.getInputVoltageDC() == null) {
        devicePoint.setInputVoltageDC(calculateMeanByPercentage(device.getInputsDC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattDC()));
      }
      if(devicePoint.getInputAmpereDC() == null && devicePoint.getInputWattDC() != null && devicePoint.getInputVoltageDC() != null) {
        devicePoint.setInputAmpereDC(devicePoint.getInputVoltageDC() <= 0 ? 0 : devicePoint.getInputWattDC() / devicePoint.getInputVoltageDC());
      }

      if(devicePoint.getInputWattAC() == null) {
        devicePoint.setInputWattAC(calculateSum(deviceInputWattsAC));
      }
      if(devicePoint.getInputVoltageAC() == null) {
        devicePoint.setInputVoltageAC(calculateMeanByPercentage(device.getInputsAC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattAC()));
      }
      if(devicePoint.getInputAmpereAC() == null && devicePoint.getInputWattAC() != null && devicePoint.getInputVoltageAC() != null) {
        devicePoint.setInputAmpereAC(devicePoint.getInputVoltageAC() <= 0 ? 0 : devicePoint.getInputWattAC() / devicePoint.getInputVoltageAC());
      }

      if(device.getOutputWattDC() == null) {
        devicePoint.setOutputWattDC(calculateSum(deviceOutputWattsDC));
      }
      if(device.getOutputVoltageDC() == null) {
        devicePoint.setOutputVoltageDC(calculateMeanByPercentage(device.getOutputsDC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattDC()));
      }
      if(device.getOutputAmpereAC() == null && devicePoint.getOutputWattDC() != null && device.getOutputVoltageDC() != null) {
        devicePoint.setOutputAmpereDC(devicePoint.getOutputVoltageDC() <= 0 ? 0 : devicePoint.getOutputWattDC() / devicePoint.getOutputVoltageDC());
      }

      if(device.getOutputWattAC() == null) {
        devicePoint.setOutputWattAC(calculateSum(deviceOutputWattsAC));
      }
      if(device.getOutputVoltageAC() == null) {
        devicePoint.setOutputVoltageAC(calculateMeanByPercentage(device.getOutputsAC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattAC()));
      }
      if(device.getOutputAmpereAC() == null && devicePoint.getOutputWattAC() != null && device.getOutputVoltageAC() != null) {
        devicePoint.setOutputAmpereAC(devicePoint.getOutputVoltageAC() <= 0 ? 0 : devicePoint.getOutputWattAC() / devicePoint.getOutputVoltageAC());
      }

      devicePoint.setInputWatt(device.getInputWatt());
      if(devicePoint.getInputWatt() == null){
        devicePoint.setInputWatt(calculateSum(Arrays.asList(devicePoint.getInputWattDC(),devicePoint.getInputWattAC())));
      }

      devicePoint.setOutputWatt(device.getOutputWatt());
      if(devicePoint.getOutputWatt() == null){
        devicePoint.setOutputWatt(calculateSum(Arrays.asList(devicePoint.getOutputWattDC(),devicePoint.getOutputWattAC())));
      }

      if(devicePoint.getOutputFrequency() == null){
        devicePoint.setOutputFrequency(calculateMean(outputDeviceFrequencies));
      }

      if(devicePoint.getInputFrequency() == null){
        devicePoint.setInputFrequency(calculateMean(inputDeviceFrequencies));
      }

      setGenericInfluxPointBaseClassAttributes(devicePoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(devicePoint);

      inputTotalKWHs = addWithZeroCheck(inputTotalKWHs,device.getInputTotalKWH());
      outputTotalKWHs = addWithZeroCheck(outputTotalKWHs,device.getOutputTotalKWH());

      devicePoints.add(devicePoint);
    }

    if(influxPoint.getInputWattDC() == null) {
      influxPoint.setInputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattDC).collect(Collectors.toList())));
    }
    if(influxPoint.getInputVoltageDC() == null) {
      influxPoint.setInputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getInputVoltageDC(),d.getInputWattDC())).collect(Collectors.toList()),influxPoint.getInputWattDC()));
    }
    if(influxPoint.getInputAmpereDC() == null && influxPoint.getInputWattDC() != null && influxPoint.getInputVoltageDC() != null) {
      influxPoint.setInputAmpereDC(influxPoint.getInputVoltageDC() <= 0 ? 0 : influxPoint.getInputWattDC() / influxPoint.getInputVoltageDC());
    }

    if(influxPoint.getInputWattAC() == null) {
      influxPoint.setInputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattAC).collect(Collectors.toList())));
    }
    if(influxPoint.getInputVoltageAC() == null) {
      influxPoint.setInputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getInputVoltageAC(),d.getInputWattAC())).collect(Collectors.toList()),influxPoint.getInputWattAC()));
    }
    if(influxPoint.getInputAmpereAC() == null && influxPoint.getInputWattAC() != null && influxPoint.getInputVoltageAC() != null) {
      influxPoint.setInputAmpereAC(influxPoint.getInputVoltageAC() <= 0 ? 0 : influxPoint.getInputWattAC() / influxPoint.getInputVoltageAC());
    }

    if(influxPoint.getOutputWattDC() == null) {
      influxPoint.setOutputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattDC).collect(Collectors.toList())));
    }
    if(influxPoint.getOutputVoltageDC() == null) {
      influxPoint.setOutputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltageDC(),d.getOutputWattDC())).collect(Collectors.toList()),influxPoint.getOutputWattDC()));
    }
    if(influxPoint.getOutputAmpereAC() == null && influxPoint.getOutputWattDC() != null && influxPoint.getOutputVoltageDC() != null) {
      influxPoint.setOutputAmpereDC(influxPoint.getOutputVoltageDC() <= 0 ? 0 : influxPoint.getOutputWattDC() / influxPoint.getOutputVoltageDC());
    }

    if(influxPoint.getOutputWattAC() == null) {
      influxPoint.setOutputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattAC).collect(Collectors.toList())));
    }
    if(influxPoint.getOutputVoltageAC() == null) {
      influxPoint.setOutputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltageAC(),d.getOutputWattAC())).collect(Collectors.toList()),influxPoint.getOutputWattAC()));
    }
    if(influxPoint.getOutputAmpereAC() == null && influxPoint.getOutputWattAC() != null && influxPoint.getOutputVoltageAC() != null) {
      influxPoint.setOutputAmpereAC(influxPoint.getOutputVoltageAC() <= 0 ? 0 : influxPoint.getOutputWattAC() / influxPoint.getOutputVoltageAC());
    }

    influxPoint.setInputWatt(solarSample.getInputWatt());
    if(influxPoint.getInputWatt() == null){
      influxPoint.setInputWatt(calculateSum(Arrays.asList(influxPoint.getInputWattDC(),influxPoint.getInputWattAC())));
    }

    influxPoint.setOutputWatt(solarSample.getOutputWatt());
    if(influxPoint.getOutputWatt() == null){
      influxPoint.setOutputWatt(calculateSum(Arrays.asList(influxPoint.getOutputWattDC(),influxPoint.getOutputWattAC())));
    }

    if(influxPoint.getInputFrequency() == null){
      influxPoint.setInputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getInputFrequency).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getOutputFrequency() == null){
      influxPoint.setOutputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputFrequency).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getTemperature() == null){
      influxPoint.setTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTemperature).filter(
          Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getBatteryTemperature() == null){
      influxPoint.setBatteryTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryTemperature).filter(
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
