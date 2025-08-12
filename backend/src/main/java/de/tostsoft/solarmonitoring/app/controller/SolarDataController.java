package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.monitoring.ApiMeterRegistry;
import de.tostsoft.solarmonitoring.lib.controller.BaseSolarDataController;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.*;
import de.tostsoft.solarmonitoring.lib.model.influx.*;
import de.tostsoft.solarmonitoring.lib.service.SolarDataValidator;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.tostsoft.solarmonitoring.app.controller.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

@RestController
public class SolarDataController extends BaseSolarDataController {

  @Autowired
  private ApiMeterRegistry apiMeterRegistry;

  @Autowired
  private SolarDataValidator solarDataValidator;

  @Autowired
  private SolarDataConverter solarDataConverter;

  @Value("${api.tokens.deye:}")
  private String deyeSunEndpointApiToken;

  @Value("${api.tokens.proxy:}")
  private String proxyEndpointApiToken;

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

  private SolarInInputACInfluxPoint convertInputDTO(InputACDTO solarSample, Long deviceId){
    return SolarInInputACInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .frequency(solarSample.getFrequency())
        .phase(solarSample.getPhase())
        .totalKWH(solarSample.getTotalKWH())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarInInputDCInfluxPoint convertInputDTO(InputDCDTO solarSample, Long deviceId){
    return SolarInInputDCInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .totalKWH(solarSample.getTotalKWH())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarBatteryInfluxPoint convertBatteryDTO(BatteryDTO solarSample, Long deviceId){
    return SolarBatteryInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .totalKWH(solarSample.getTotalKWH())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }


  private SolarOutputDCInfluxPoint convertOutputDTO(OutputDCDTO solarSample, Long deviceId){
    return SolarOutputDCInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .totalKWH(solarSample.getTotalKWH())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private SolarOutputACInfluxPoint convertOutputDTO(OutputACDTO solarSample, Long deviceId){
    return SolarOutputACInfluxPoint.builder()
        .watt(solarSample.getWatt())
        .ampere(solarSample.getAmpere())
        .voltage(solarSample.getVoltage())
        .totalKWH(solarSample.getTotalKWH())
        .frequency(solarSample.getFrequency())
        .phase(solarSample.getPhase())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  static public Float calculateMean(List<Float> values){
    if(values.isEmpty()){
      return null;
    }
    float res = 0;
    int num = 0;
    for (Float value : values) {
      if(value == null){
        continue;
      }
      res += value;
      num++;
    }
    if(num == 0){
      return null;
    }
    return res/num;
  }

  static public Float calculateMeanByPercentage(List<Pair<Float,Float>> values,Float max){
    if(max == null){
      return null;
    }
    if(values.isEmpty()){
      return null;
    }
    if(max == 0){
      return calculateMean(values.stream().map(Pair::getLeft).collect(Collectors.toList()));
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

  static public Float calculateSum(List<Float> values){
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

  static public Float addWithZeroCheck(Float old,Float toAdd){
    if(toAdd == null){
      return old;
    }
    if(old == null){
      return toAdd;
    }
    return old+toAdd;
  }

  static public Integer addWithZeroCheck(Integer old,Integer toAdd){
    if(toAdd == null){
      return old;
    }
    if(old == null){
      return toAdd;
    }
    return old+toAdd;
  }

  private List<GenericInfluxPoint> convertToInfluxPoint(final SampleDTO solarSample, String systemId,boolean combineTotalValuesAfterwards){

    List<GenericInfluxPoint> res = new ArrayList<>();

    List<SolarDeviceInfluxPoint> devicePoints = new ArrayList<>();
    Float inputDCTotalKWHs = null;
    Float outputDCTotalKWHs = null;
    Float inputACTotalKWHs = null;
    Float outputACTotalKWHs = null;
    Float batteryTotalKWHs = null;

    long timestamp = solarSample.getTimestamp();
    if(solarSample.getTimeUnit() != null){
      timestamp = TimeUnit.MILLISECONDS.convert(solarSample.getTimestamp(), solarSample.getTimeUnit());
    }

    for (DeviceDTO device : solarSample.getDevices()) {

      Float deviceInputDCTotalKWHs = null;
      Float deviceOutputDCTotalKWHs = null;
      Float deviceInputACTotalKWHs = null;
      Float deviceOutputACTotalKWHs = null;
      Float deviceBatteryTotalKWHs = null;
      Integer numActiveConnectsions = null;

      for (var input : device.getInputsDC()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            timestamp, systemId);
        res.add(point);

        deviceInputDCTotalKWHs = addWithZeroCheck(deviceInputDCTotalKWHs,input.getTotalKWH());
        numActiveConnectsions = addWithZeroCheck(numActiveConnectsions,1);
      }

      for (var input : device.getInputsAC()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                timestamp, systemId);
        res.add(point);

        deviceInputACTotalKWHs = addWithZeroCheck(deviceInputACTotalKWHs,input.getTotalKWH());
        numActiveConnectsions = addWithZeroCheck(numActiveConnectsions,1);
      }

      for (var battery : device.getBatteries()) {
        var point = convertBatteryDTO(battery,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                timestamp, systemId);
        res.add(point);

        deviceBatteryTotalKWHs = addWithZeroCheck(deviceBatteryTotalKWHs,battery.getTotalKWH());
        numActiveConnectsions = addWithZeroCheck(numActiveConnectsions,1);
      }


      for (var output : device.getOutputsDC()) {
        var point = convertOutputDTO(output,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                timestamp, systemId);
        res.add(point);

        deviceOutputDCTotalKWHs = addWithZeroCheck(deviceOutputDCTotalKWHs,output.getTotalKWH());
        numActiveConnectsions = addWithZeroCheck(numActiveConnectsions,1);
      }

      for (var output : device.getOutputsAC()) {
        var point = convertOutputDTO(output,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                timestamp, systemId);
        res.add(point);

        deviceOutputACTotalKWHs = addWithZeroCheck(deviceOutputACTotalKWHs,output.getTotalKWH());
        numActiveConnectsions = addWithZeroCheck(numActiveConnectsions,1);
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
          .inputDCTotalKWH(device.getInputDCTotalKWH())
          .outputDCTotalKWH(device.getOutputDCTotalKWH())
          .inputACTotalKWH(device.getInputACTotalKWH())
          .outputACTotalKWH(device.getOutputACTotalKWH())
          .inputTotalKWH(device.getInputTotalKWH())
          .outputTotalKWH(device.getOutputTotalKWH())
          .totalOH(device.getTotalOH())
          .temperature(device.getTemperature())
          .batteryTemperature(device.getBatteryTemperature())
          .batteryVoltage(device.getBatteryVoltage())
          .batteryAmpere(device.getBatteryAmpere())
          .batteryWatt(device.getBatteryWatt())
          .inputFrequency(device.getInputFrequency())
          .outputFrequency(device.getOutputFrequency())
          .batteryPercentage(device.getBatteryPercentage())
          .id(device.getId())
          .build();

      if(devicePoint.getInputWattDC() == null) {
        devicePoint.setInputWattDC(calculateSum(device.getInputsDC().stream().map(InputDCDTO::getWatt).collect(Collectors.toList())));
      }
      if(devicePoint.getInputVoltageDC() == null) {
        devicePoint.setInputVoltageDC(calculateMeanByPercentage(device.getInputsDC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattDC()));
      }
      if(devicePoint.getInputAmpereDC() == null && devicePoint.getInputWattDC() != null && devicePoint.getInputVoltageDC() != null) {
        devicePoint.setInputAmpereDC(devicePoint.getInputVoltageDC() <= 0 ? 0 : devicePoint.getInputWattDC() / devicePoint.getInputVoltageDC());
      }

      if(devicePoint.getInputWattAC() == null) {
        devicePoint.setInputWattAC(calculateSum(device.getInputsAC().stream().map(InputACDTO::getWatt).collect(Collectors.toList())));
      }
      if(devicePoint.getInputVoltageAC() == null) {
        devicePoint.setInputVoltageAC(calculateMeanByPercentage(device.getInputsAC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattAC()));
      }
      if(devicePoint.getInputAmpereAC() == null && devicePoint.getInputWattAC() != null && devicePoint.getInputVoltageAC() != null) {
        devicePoint.setInputAmpereAC(devicePoint.getInputVoltageAC() <= 0 ? 0 : devicePoint.getInputWattAC() / devicePoint.getInputVoltageAC());
      }

      if(devicePoint.getBatteryWatt() == null) {
        devicePoint.setBatteryWatt(calculateSum(device.getBatteries().stream().map(BatteryDTO::getWatt).collect(Collectors.toList())));
      }
      if(devicePoint.getBatteryVoltage() == null) {
        devicePoint.setBatteryVoltage(calculateMean(device.getBatteries().stream().map(BatteryDTO::getVoltage).collect(Collectors.toList())));
      }
      if(devicePoint.getBatteryAmpere() == null && devicePoint.getBatteryWatt() != null && devicePoint.getBatteryVoltage() != null) {
        devicePoint.setBatteryAmpere(devicePoint.getBatteryWatt() / devicePoint.getBatteryVoltage());
      }

      if(devicePoint.getOutputWattDC() == null) {
        devicePoint.setOutputWattDC(calculateSum(device.getOutputsDC().stream().map(OutputDCDTO::getWatt).collect(Collectors.toList())));
      }
      if(devicePoint.getOutputVoltageDC() == null) {
        devicePoint.setOutputVoltageDC(calculateMeanByPercentage(device.getOutputsDC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattDC()));
      }
      if(devicePoint.getOutputAmpereDC() == null && devicePoint.getOutputWattDC() != null && devicePoint.getOutputVoltageDC() != null) {
        devicePoint.setOutputAmpereDC(devicePoint.getOutputVoltageDC() <= 0 ? 0 : devicePoint.getOutputWattDC() / devicePoint.getOutputVoltageDC());
      }

      if(devicePoint.getOutputWattAC() == null) {
        devicePoint.setOutputWattAC(calculateSum(device.getOutputsAC().stream().map(OutputACDTO::getWatt).collect(Collectors.toList())));
      }
      if(devicePoint.getOutputVoltageAC() == null) {
        devicePoint.setOutputVoltageAC(calculateMeanByPercentage(device.getOutputsAC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattAC()));
      }
      if(devicePoint.getOutputAmpereAC() == null && devicePoint.getOutputWattAC() != null && devicePoint.getOutputVoltageAC() != null) {
        devicePoint.setOutputAmpereAC(devicePoint.getOutputVoltageAC() <= 0 ? 0 : devicePoint.getOutputWattAC() / devicePoint.getOutputVoltageAC());
      }

      if(devicePoint.getInputWatt() == null){
        devicePoint.setInputWatt(calculateSum(Arrays.asList(devicePoint.getInputWattDC(),devicePoint.getInputWattAC())));
      }

      if(devicePoint.getOutputWatt() == null){
        devicePoint.setOutputWatt(calculateSum(Arrays.asList(devicePoint.getOutputWattDC(),devicePoint.getOutputWattAC())));
      }

      if(devicePoint.getInputFrequency() == null){
        devicePoint.setInputFrequency(calculateMean(device.getInputsAC().stream().map(InputACDTO::getFrequency).collect(Collectors.toList())));
      }

      if(devicePoint.getOutputFrequency() == null){
        devicePoint.setOutputFrequency(calculateMean(device.getOutputsAC().stream().map(OutputACDTO::getFrequency).collect(Collectors.toList())));
      }

      if(devicePoint.getInputACTotalKWH() == null){
        devicePoint.setInputACTotalKWH(deviceInputACTotalKWHs);
      }
      if(devicePoint.getOutputACTotalKWH() == null){
        devicePoint.setOutputACTotalKWH(deviceOutputACTotalKWHs);
      }
      if(devicePoint.getInputDCTotalKWH() == null){
        devicePoint.setInputDCTotalKWH(deviceInputDCTotalKWHs);
      }
      if(devicePoint.getOutputDCTotalKWH() == null){
        devicePoint.setOutputDCTotalKWH(deviceOutputDCTotalKWHs);
      }
      if(devicePoint.getInputTotalKWH() == null){
        devicePoint.setInputTotalKWH(addWithZeroCheck(devicePoint.getInputACTotalKWH(),devicePoint.getInputDCTotalKWH()));
      }
      if(devicePoint.getOutputTotalKWH() == null){
        devicePoint.setOutputTotalKWH(addWithZeroCheck(devicePoint.getOutputACTotalKWH(),devicePoint.getOutputDCTotalKWH()));
      }

      devicePoint.setNumActiveConnections(numActiveConnectsions);

      setGenericInfluxPointBaseClassAttributes(devicePoint,solarSample.getDuration(),timestamp,systemId);

      res.add(devicePoint);

      devicePoints.add(devicePoint);

      //some values of main device
      inputACTotalKWHs = addWithZeroCheck(inputACTotalKWHs,devicePoint.getInputACTotalKWH());
      inputDCTotalKWHs = addWithZeroCheck(inputDCTotalKWHs,devicePoint.getInputDCTotalKWH());
      outputDCTotalKWHs = addWithZeroCheck(outputDCTotalKWHs,devicePoint.getOutputDCTotalKWH());
      outputACTotalKWHs = addWithZeroCheck(outputACTotalKWHs,devicePoint.getOutputACTotalKWH());
      batteryTotalKWHs = addWithZeroCheck(outputACTotalKWHs,devicePoint.getBatteryTotalKWH());
    }

    if(!combineTotalValuesAfterwards){

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
              .outputVoltageAC(solarSample.getOutputVoltageAC())
              .outputAmpereAC(solarSample.getOutputAmpereAC())
              .outputWattAC(solarSample.getOutputWattAC())
              .inputDCTotalKWH(solarSample.getInputDCTotalKWH())
              .outputDCTotalKWH(solarSample.getOutputDCTotalKWH())
              .inputACTotalKWH(solarSample.getInputACTotalKWH())
              .outputACTotalKWH(solarSample.getOutputACTotalKWH())
              .inputTotalKWH(solarSample.getInputTotalKWH())
              .outputTotalKWH(solarSample.getOutputTotalKWH())
              .totalOH(solarSample.getTotalOH())
              .outputFrequency(solarSample.getOutputFrequency())
              .inputFrequency(solarSample.getInputFrequency())
              .temperature(solarSample.getTemperature())
              .batteryTemperature(solarSample.getBatteryTemperature())
              .batteryVoltage(solarSample.getBatteryVoltage())
              .batteryAmpere(solarSample.getBatteryAmpere())
              .batteryWatt(solarSample.getBatteryWatt())
              .batteryPercentage(solarSample.getBatteryPercentage())
              .build();

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

      if(influxPoint.getBatteryWatt() == null) {
        influxPoint.setBatteryWatt(calculateSum(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryWatt).collect(Collectors.toList())));
      }
      if(influxPoint.getBatteryVoltage() == null) {
        influxPoint.setBatteryVoltage(calculateMean(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryVoltage).collect(Collectors.toList())));
      }
      if(influxPoint.getBatteryAmpere() == null && influxPoint.getBatteryWatt() != null && influxPoint.getBatteryVoltage() != null) {
        influxPoint.setBatteryAmpere(influxPoint.getBatteryWatt() / influxPoint.getBatteryVoltage());
      }

      if(influxPoint.getOutputWattDC() == null) {
        influxPoint.setOutputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattDC).collect(Collectors.toList())));
      }
      if(influxPoint.getOutputVoltageDC() == null) {
        influxPoint.setOutputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getOutputVoltageDC(),d.getOutputWattDC())).collect(Collectors.toList()),influxPoint.getOutputWattDC()));
      }
      if(influxPoint.getOutputAmpereDC() == null && influxPoint.getOutputWattDC() != null && influxPoint.getOutputVoltageDC() != null) {
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

      if(influxPoint.getBatteryPercentage() == null){
        influxPoint.setBatteryPercentage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryPercentage).filter(
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

      if(influxPoint.getInputACTotalKWH() == null){
        influxPoint.setInputACTotalKWH(inputACTotalKWHs);
      }
      if(influxPoint.getOutputACTotalKWH() == null){
        influxPoint.setOutputACTotalKWH(outputACTotalKWHs);
      }
      if(influxPoint.getInputDCTotalKWH() == null){
        influxPoint.setInputDCTotalKWH(inputDCTotalKWHs);
      }
      if(influxPoint.getOutputDCTotalKWH() == null){
        influxPoint.setOutputDCTotalKWH(outputDCTotalKWHs);
      }
      if(influxPoint.getBatteryTotalKWH() == null){
        influxPoint.setBatteryTotalKWH(batteryTotalKWHs);
      }
      if(solarSample.getInputTotalKWH() == null){
        influxPoint.setInputTotalKWH(addWithZeroCheck(influxPoint.getInputACTotalKWH(),influxPoint.getInputDCTotalKWH()));
      }
      if(solarSample.getOutputTotalKWH() == null){
        influxPoint.setOutputTotalKWH(addWithZeroCheck(influxPoint.getOutputACTotalKWH(),influxPoint.getOutputDCTotalKWH()));
      }

      if(solarSample.getDevices() != null){
        influxPoint.setNumActiveDevices(solarSample.getDevices().size());
      }

      setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),timestamp,systemId);

      res.add(influxPoint);

    }

    return res;
  }

  public void PostDevice(String systemId, SampleDTO solarSample, String clientToken) {
    apiMeterRegistry.incrementApiEndpointCallData();
    solarDataConverter.genericHandleMulti(systemId,solarSample,clientToken,(sample,solarSystem)->{
      solarDataValidator.validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId,Boolean.TRUE.equals(solarSystem.getCalculateCombinedValuesAfterwards()));
    });
    apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
  }

  //post mapping in base class
  public void PostDeviceMult(String systemId, List<SampleDTO> solarSamples, String clientToken) {
    apiMeterRegistry.incrementApiEndpointCallData();
    solarDataConverter.genericHandleMultipleMulti(systemId,solarSamples,clientToken,(sample,solarSystem)->{
      solarDataValidator.validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId,Boolean.TRUE.equals(solarSystem.getCalculateCombinedValuesAfterwards()));
    });
    apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
  }

  //post mapping in base class
  public void PostDeviceDeye(String serialId,SampleDTO solarSample, String clientToken) {

    if(StringUtils.isEmpty(deyeSunEndpointApiToken)){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not activated");
    }

    if(!StringUtils.equals(deyeSunEndpointApiToken,clientToken)){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This Endpoint requires Authentication");
    }

    apiMeterRegistry.incrementApiEndpointCallData();
    long serial;

    try{
      serial = Long.parseLong(serialId);
    }catch (NumberFormatException exception){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serialId must be numeric");
    }
    solarDataConverter.genericHandleDeye(serial,solarSample,(system,sample)->{
      solarDataValidator.validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,system.getId(),Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards()));
    });

    apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
  }

  @PostMapping("/proxy")
  public ResponseEntity<String> PostDeviceProxy(@RequestParam String systemId, @RequestBody @Valid List<SampleDTO> solarSamples, @RequestHeader String proxyToken){

    if(StringUtils.isEmpty(proxyToken)){
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not activated");
    }

    if(!StringUtils.equals(proxyEndpointApiToken,proxyToken)){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This Endpoint requires Authentication");
    }

    LOG.info("Proxy Endpoint called with "+solarSamples.size()+" samples on system: "+systemId);

    var notOk = new AtomicInteger(0);

    apiMeterRegistry.incrementApiEndpointCallData();

    solarDataConverter.genericHandleProxy(systemId,solarSamples,(sample,solarSystem)->{
      try {
        LOG.debug("Proxy Endpoint sample: " + sample);
        solarDataValidator.validateAndFillMissing(sample);
        return convertToInfluxPoint(sample, systemId, Boolean.TRUE.equals(solarSystem.getCalculateCombinedValuesAfterwards()));
      }catch (Exception ex){
        LOG.warn("Could not handle proxy solardata",ex);
        notOk.set(notOk.get() + 1);
      }
      return new ArrayList<>();
    });
    apiMeterRegistry.incrementApiEndpointCallDataSuccessful();

    return new ResponseEntity<String>(notOk.get() > 0 ? "Request not ok: "+notOk.get():"",HttpStatus.OK);
  }
}
