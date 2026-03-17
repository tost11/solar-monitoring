package de.tostsoft.solarmonitoring.lib.service;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@Service
public class SolarDataValidator {

  Float sumWithZero(Float... floats){
    Float ret = null;
    for (Float f : floats) {
      if(ret == null){
        ret = f;
      }else{
        if(f != null){
          ret += f;
        }
      }
    }
    return ret;
  }

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


  private void validateAndFillMissing(GridDTO sample){
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
    if (device.getOutputWattAC() == null && device.getOutputAmpereAC() != null && device.getOutputVoltageAC() != null) {
      device.setOutputWattAC(device.getOutputAmpereAC() * device.getOutputVoltageAC());
    }
    if (device.getBatteryWatt() == null && device.getBatteryAmpere() != null && device.getBatteryVoltage() != null) {
      device.setBatteryWatt(device.getBatteryAmpere() * device.getBatteryVoltage());
    }

    //for ampere
    if (device.getInputAmpereDC() == null && device.getInputWattDC() != null && device.getInputVoltageDC() != null) {
      device.setInputAmpereDC(device.getInputVoltageDC() == 0 ? 0 : device.getInputWattDC() / device.getInputVoltageDC());
    }
    if (device.getOutputAmpereAC() == null && device.getOutputWattAC() != null && device.getOutputVoltageAC() != null) {
      device.setOutputAmpereAC(device.getOutputVoltageAC() == 0 ? 0 : device.getOutputWattAC() / device.getOutputVoltageAC());
    }
    if (device.getBatteryAmpere() == null && device.getBatteryWatt() != null && device.getBatteryVoltage() != null) {
      device.setBatteryAmpere(device.getBatteryVoltage() == 0 ? 0 : device.getBatteryWatt() / device.getBatteryVoltage());
    }

    //for voltage
    if (device.getInputVoltageDC() == null && device.getInputWattDC() != null && device.getInputAmpereDC() != null) {
      device.setInputVoltageDC(device.getInputAmpereDC() == 0 ? null : device.getInputWattDC() / device.getInputAmpereDC());
    }
    if (device.getOutputVoltageAC() == null && device.getOutputWattAC() != null && device.getOutputAmpereAC() != null) {
      device.setOutputVoltageAC(device.getOutputAmpereAC() == 0 ? null : device.getOutputWattAC() / device.getOutputAmpereAC());
    }

    //total values will be calculated by converting

    if (device.getBatteryVoltage() == null && device.getBatteryWatt() != null && device.getBatteryAmpere() != null) {
      device.setBatteryVoltage(device.getBatteryAmpere() == 0 ? null : device.getBatteryWatt() / device.getBatteryAmpere());
      if(device.getBatteryVoltage() < 0){
        device.setBatteryVoltage(device.getBatteryVoltage() * -1);
      }
    }
  }

  public void validateAndFillMissing(SampleDTO solarSample){
    validateSolarSampleDTO(solarSample);

    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
      solarSample.setTimeUnit(TimeUnit.MILLISECONDS);
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
    if (solarSample.getOutputWattAC() == null && solarSample.getOutputAmpereAC() != null && solarSample.getOutputVoltageAC() != null) {
      solarSample.setOutputWattAC(solarSample.getOutputAmpereAC() * solarSample.getOutputVoltageAC());
    }
    if (solarSample.getBatteryWatt() == null && solarSample.getBatteryAmpere() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryWatt(solarSample.getBatteryAmpere() * solarSample.getBatteryVoltage());
    }

    //for ampere
    if (solarSample.getInputAmpereDC() == null && solarSample.getInputWattDC() != null && solarSample.getInputVoltageDC() != null) {
      solarSample.setInputAmpereDC(solarSample.getInputVoltageDC() == 0 ? 0 : solarSample.getInputWattDC() / solarSample.getInputVoltageDC());
    }
    if (solarSample.getOutputAmpereAC() == null && solarSample.getOutputWattAC() != null && solarSample.getOutputVoltageAC() != null) {
      solarSample.setOutputVoltageAC(solarSample.getOutputVoltageAC() == 0 ? 0 : solarSample.getOutputWattAC() / solarSample.getOutputVoltageAC());
    }
    if (solarSample.getBatteryAmpere() == null && solarSample.getBatteryWatt() != null && solarSample.getBatteryVoltage() != null) {
      solarSample.setBatteryAmpere(solarSample.getBatteryVoltage() == 0 ? 0 : solarSample.getBatteryWatt() / solarSample.getBatteryVoltage());
    }

    //for voltage
    if (solarSample.getInputVoltageDC() == null && solarSample.getInputWattDC() != null && solarSample.getInputAmpereDC() != null) {
      solarSample.setInputVoltageDC(solarSample.getInputAmpereDC() == 0 ? null : solarSample.getInputWattDC() / solarSample.getInputAmpereDC());
    }
    if (solarSample.getOutputVoltageAC() == null && solarSample.getOutputWattAC() != null && solarSample.getOutputAmpereAC() != null) {
      solarSample.setOutputVoltageAC(solarSample.getOutputAmpereAC() == 0 ? null : solarSample.getOutputWattAC() / solarSample.getOutputAmpereAC());
    }

    //total values will be calculated by converting

    if (solarSample.getBatteryVoltage() == null && solarSample.getBatteryWatt() != null && solarSample.getBatteryAmpere() != null) {
      solarSample.setBatteryVoltage(solarSample.getBatteryAmpere() == 0 ? null : solarSample.getBatteryWatt() / solarSample.getBatteryAmpere());
      if(solarSample.getBatteryVoltage() < 0){
        solarSample.setBatteryVoltage(solarSample.getBatteryVoltage() * -1);
      }
    }

    // Grid field validation and auto-calculation
    // for watt
    if (solarSample.getGridWatt() == null && solarSample.getGridAmpere() != null && solarSample.getGridVoltage() != null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
    }

    // for ampere
    if (solarSample.getGridAmpere() == null && solarSample.getGridWatt() != null && solarSample.getGridVoltage() != null) {
      solarSample.setGridAmpere(solarSample.getGridVoltage() == 0 ? 0 : solarSample.getGridWatt() / solarSample.getGridVoltage());
    }

    // for voltage
    if (solarSample.getGridVoltage() == null && solarSample.getGridWatt() != null && solarSample.getGridAmpere() != null) {
      solarSample.setGridVoltage(solarSample.getGridAmpere() == 0 ? null : solarSample.getGridWatt() / solarSample.getGridAmpere());
    }

    var tmpDeviceIds = new HashSet<Long>();

    for (var device : solarSample.getDevices()) {

      validateThrow(tmpDeviceIds.contains(device.getId()),"Two devices with the same Id Found "+device.getId());
      tmpDeviceIds.add(device.getId());
      validateDeviceDTO(device);

      if(device.getInputsDC() == null){
        device.setInputsDC(new ArrayList<>());
      }
      if(device.getInputsAC() == null){
        device.setInputsAC(new ArrayList<>());
      }
      if(device.getBatteries() == null){
        device.setBatteries(new ArrayList<>());
      }
      if(device.getOutputsDC() == null){
        device.setOutputsDC(new ArrayList<>());
      }
      if(device.getOutputsAC() == null){
        device.setOutputsAC(new ArrayList<>());
      }
      if(device.getGrids() == null){
        device.setGrids(new ArrayList<>());
      }

      var ids = new HashSet<Long>();
      for (var input : device.getInputsDC()) {
        validateThrow(ids.contains(input.getId()),"Two dc inputs on device "+device.getId()+" with the same Id Found "+input.getId());
        ids.add(input.getId());
        validateAndFillMissing(input);
      };

      ids.clear();
      for (var input : device.getInputsAC()) {
        validateThrow(ids.contains(input.getId()),"Two ac inputs on device "+device.getId()+" with the same Id Found "+input.getId());
        ids.add(input.getId());
        validateAndFillMissing(input);
      };

      ids.clear();
      for (var output : device.getOutputsDC()) {
        validateThrow(ids.contains(output.getId()),"Two dc outputs on device "+device.getId()+" with the same Id Found "+output.getId());
        ids.add(output.getId());
        validateAndFillMissing(output);
      };

      ids.clear();
      for (var output : device.getOutputsAC()) {
        validateThrow(ids.contains(output.getId()),"Two ac outputs on device "+device.getId()+" with the same Id Found "+output.getId());
        ids.add(output.getId());
        validateAndFillMissing(output);
      };

      ids.clear();
      for (var battery : device.getBatteries()) {
        validateThrow(ids.contains(battery.getId()),"Two Batteries on device "+device.getId()+" with the same Id Found "+battery.getId());
        ids.add(battery.getId());
        validateAndFillMissing(battery);
      };

      ids.clear();
      for (var grid : device.getGrids()) {
        validateThrow(ids.contains(grid.getId()),"Two Grids on device "+device.getId()+" with the same Id Found "+grid.getId());
        ids.add(grid.getId());
        validateAndFillMissing(grid);
      };
    }
  }

}
