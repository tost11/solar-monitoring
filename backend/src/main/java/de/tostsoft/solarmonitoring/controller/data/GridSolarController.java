package de.tostsoft.solarmonitoring.controller.data;

import static de.tostsoft.solarmonitoring.controller.data.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.DeviceGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.SimpleGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridInputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridOutputDTO;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxInputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxOutputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxPoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
public class GridSolarController {

  @Autowired
  private SolarDataConverter solarDataConverter;

  private void validateAndFillMissing(GridInputDTO solarSample){
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

  private void validateAndFillMissing(GridOutputDTO solarSample){
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

  private GridSolarInfluxInputPoint convertInputDTO(GridInputDTO solarSample,Long deviceId){
    return GridSolarInfluxInputPoint.builder()
        .chargeWatt(solarSample.getWatt())
        .chargeAmpere(solarSample.getAmpere())
        .chargeVoltage(solarSample.getVoltage())
        .id(solarSample.getId())
        .deviceId(deviceId)
        .build();
  }

  private GridSolarInfluxOutputPoint convertOutputDTO(GridOutputDTO solarSample,Long deviceId){
    return GridSolarInfluxOutputPoint.builder()
        .gridWatt(solarSample.getWatt())
        .gridAmpere(solarSample.getAmpere())
        .gridVoltage(solarSample.getVoltage())
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

  // --------------------------------------- simple grid -------------------------------------------------

  private void validateAndFillMissing(SimpleGridSolarSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }
    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }

    if(solarSample.getChargeVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeVoltage must be above or zero");
    }
    if(solarSample.getChargeAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeAmpere must be above or zero");
    }

    if(solarSample.getGridVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridVoltage must be above zero");
    }
    if(solarSample.getGridAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridAmpere must be above or zero");
    }

    if (solarSample.getChargeWatt() == null) {
      solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
    }else if(solarSample.getChargeWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeWatt must be above or zero");
    }
    if (solarSample.getGridWatt() == null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
    }else if(solarSample.getGridWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridWatt must be above or zero");
    }
  }

  private GridSolarInfluxPoint convertToInfluxPoint(SimpleGridSolarSampleDTO solarSample,long systemId){
    var influxPoint = GridSolarInfluxPoint.builder()
        .chargeVoltage(solarSample.getChargeVoltage())
        .chargeAmpere(solarSample.getChargeAmpere())
        .chargeWatt(solarSample.getChargeWatt())
        .gridVoltage(solarSample.getGridVoltage())
        .gridAmpere(solarSample.getGridAmpere())
        .gridWatt(solarSample.getGridWatt())
        .totalKWH(solarSample.getTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .frequency(solarSample.getFrequency())
        .deviceTemperature(solarSample.getDeviceTemperature())
        .phase(solarSample.getPhase())
        .totalKWH(solarSample.getTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .build();

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    return influxPoint;
  }

  @PostMapping("/simple")
  public void PostDataSimple(@RequestParam long systemId, @Valid @RequestBody SimpleGridSolarSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("simple/mult")
  public void PostDataSimpleMult(@RequestParam long systemId, @Valid @RequestBody List<SimpleGridSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  // ------------------------------------------- input output ----------------------------------------------------------------------------

  /*private void validateAndFillMissing(InputOutputGridSolarSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }

    if(CollectionUtils.isEmpty(solarSample.getInputs())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs empty");
    }

    if(CollectionUtils.isEmpty(solarSample.getOutputs())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputs empty");
    }

    if(solarSample.getInputs().stream().anyMatch(v->v.getId() <= 0) ||
        solarSample.getOutputs().stream().anyMatch(v->v.getId() <= 0)){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
    }

    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }

    if(solarSample.getChargeVoltage() != null && solarSample.getChargeVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeVoltage must be above or zero");
    }
    if(solarSample.getChargeAmpere() != null && solarSample.getChargeAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeAmpere must be above or zero");
    }

    if(solarSample.getGridVoltage() != null && solarSample.getGridVoltage() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridVoltage must be above zero");
    }
    if(solarSample.getGridAmpere() != null && solarSample.getGridAmpere() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridAmpere must be above or zero");
    }

    if (solarSample.getChargeWatt() == null){
      if(solarSample.getChargeAmpere() != null && solarSample.getChargeVoltage() != null) {
        solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
      }
    }else if(solarSample.getChargeWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeWatt must be above or zero");
    }
    if (solarSample.getGridWatt() == null){
      if(solarSample.getGridAmpere() != null && solarSample.getGridVoltage() == null) {
        solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
      }
    }else if(solarSample.getGridWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridWatt must be above or zero");
    }

    solarSample.getInputs().forEach(this::validateAndFillMissing);
    solarSample.getOutputs().forEach(this::validateAndFillMissing);
  }

  private List<GenericInfluxPoint> convertToInfluxPoint(InputOutputGridSolarSampleDTO solarSample,long systemId){

    List<GenericInfluxPoint> res = new ArrayList<>();

    var influxPoint = GridSolarInfluxPoint.builder()
        .chargeVoltage(solarSample.getChargeVoltage())
        .chargeAmpere(solarSample.getChargeAmpere())
        .chargeWatt(solarSample.getChargeWatt())
        .gridVoltage(solarSample.getGridVoltage())
        .gridAmpere(solarSample.getGridAmpere())
        .gridWatt(solarSample.getGridWatt())
        .totalKWH(solarSample.getTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .frequency(solarSample.getFrequency())
        .deviceTemperature(solarSample.getDeviceTemperature())
        .totalKWH(solarSample.getTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .build();

    List<Float> inputWatts = new ArrayList<>();
    List<Float> inputVoltages = new ArrayList<>();

    List<Float> outputWatts = new ArrayList<>();
    List<Float> outputVoltages = new ArrayList<>();

    List<Float> outputFrequencies = new ArrayList<>();

    for (GridInputDTO input : solarSample.getInputs()) {
      var point = convertInputDTO(input,null);
      setGenericInfluxPointBaseClassAttributes(point,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(point);

      inputWatts.add(input.getWatt());
      inputVoltages.add(input.getVoltage());
    }

    for (GridOutputDTO output : solarSample.getOutputs()) {
      var point = convertOutputDTO(output,null);
      setGenericInfluxPointBaseClassAttributes(point,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(point);

      outputWatts.add(output.getWatt());
      outputVoltages.add(output.getVoltage());
      if(output.getFrequency() != null) {
        outputFrequencies.add(output.getFrequency());
      }
    }



    if(influxPoint.getChargeWatt() == null){
      influxPoint.setChargeWatt(calculateMean(inputWatts));
      influxPoint.setChargeVoltage(calculateMean(inputVoltages));
      influxPoint.setChargeAmpere(influxPoint.getChargeWatt()/influxPoint.getChargeVoltage());
    }else if(solarSample.getChargeWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ChargeWatt must be above or zero");
    }

    if(influxPoint.getGridWatt() == null){
      influxPoint.setGridWatt(calculateMean(outputWatts));
      influxPoint.setGridVoltage(calculateMean(outputVoltages));
      influxPoint.setGridAmpere(influxPoint.getGridWatt()/influxPoint.getGridVoltage());
    }else if(solarSample.getGridWatt() < 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"GridWatt must be above or zero");
    }

    if(influxPoint.getFrequency() != null && !outputFrequencies.isEmpty()){
      influxPoint.setFrequency(calculateMean(outputFrequencies));
    }

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    res.add(influxPoint);

    return res;
  }

  @PostMapping("/io")
  public void PostDataInputOutput(@RequestParam long systemId, @RequestBody InputOutputGridSolarSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMulti(systemId,solarSample,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/io/mult")
  public void PostDataInputOutputMult(@RequestParam long systemId, @RequestBody List<InputOutputGridSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultipleMulti(systemId,solarSamples,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }*/

  // ---------------------------------------------------- device ------------------------------------------------------


  private void validateDeviceGridSolarSampleDTO(final DeviceGridSolarSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }
    if(CollectionUtils.isEmpty(solarSample.getDevices())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs empty");
    }
    if(solarSample.getChargeVoltage() == null){
      if(solarSample.getChargeAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getChargeVoltage() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage must be above or zero");
      }
      if(solarSample.getChargeAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(solarSample.getChargeAmpere() == null){
      if(solarSample.getChargeVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getChargeAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeAmpere must be above or zero");
      }
      if(solarSample.getChargeVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }

    if(solarSample.getGridVoltage() == null){
      if(solarSample.getGridAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getGridVoltage() <= 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage must be above zero");
      }
      if(solarSample.getGridAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(solarSample.getGridAmpere() == null){
      if(solarSample.getGridVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(solarSample.getGridAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridAmpere must be above or zero");
      }
      if(solarSample.getGridVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
  }

  private void validateGridDeviceDTO(GridDeviceDTO device){

    if(device.getId() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
    }

    if(device.getChargeVoltage() == null){
      if(device.getChargeAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getChargeVoltage() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage must be above or zero");
      }
      if(device.getChargeAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(device.getChargeAmpere() == null){
      if(device.getChargeVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getChargeAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeAmpere must be above or zero");
      }
      if(device.getChargeVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChargeVoltage and ChargeAmpere must both be set or unset");
      }
    }

    if(device.getGridVoltage() == null){
      if(device.getGridAmpere() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getGridVoltage() <= 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage must be above zero");
      }
      if(device.getGridAmpere() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
    if(device.getGridAmpere() == null){
      if(device.getGridVoltage() != null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }else{
      if(device.getGridAmpere() < 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridAmpere must be above or zero");
      }
      if(device.getGridVoltage() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GridVoltage and ChargeAmpere must both be set or unset");
      }
    }
  }

  private void validateAndFillMissing(DeviceGridSolarSampleDTO solarSample){
    validateDeviceGridSolarSampleDTO(solarSample);


    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }

    if (solarSample.getChargeWatt() == null && solarSample.getChargeAmpere() != null && solarSample.getChargeVoltage() != null) {
      solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
    }
    if (solarSample.getGridWatt() == null && solarSample.getGridAmpere() != null && solarSample.getGridVoltage() != null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
    }


    for (GridDeviceDTO device : solarSample.getDevices()) {

      validateGridDeviceDTO(device);

      if (device.getChargeWatt() == null && device.getChargeAmpere() != null && device.getChargeVoltage() != null) {
        device.setChargeWatt(device.getChargeAmpere() * device.getChargeVoltage());
      }
      if (device.getGridWatt() == null && device.getGridAmpere() != null && device.getGridVoltage() != null) {
        device.setGridWatt(device.getGridAmpere() * device.getGridVoltage());
      }

      device.getInputs().forEach(this::validateAndFillMissing);
      device.getOutputs().forEach(this::validateAndFillMissing);
    }
  }

  private List<GenericInfluxPoint> convertToInfluxPoint(DeviceGridSolarSampleDTO solarSample,long systemId){

    List<GenericInfluxPoint> res = new ArrayList<>();

    var influxPoint = GridSolarInfluxPoint.builder()
        .chargeVoltage(solarSample.getChargeVoltage())
        .chargeAmpere(solarSample.getChargeAmpere())
        .chargeWatt(solarSample.getChargeWatt())
        .gridVoltage(solarSample.getGridVoltage())
        .gridAmpere(solarSample.getGridAmpere())
        .gridWatt(solarSample.getGridWatt())
        .totalKWH(solarSample.getTotalKWH())
        .totalOH(solarSample.getTotalOH())
        .frequency(solarSample.getFrequency())
        .deviceTemperature(solarSample.getDeviceTemperature())
        .build();

    List<GridSolarInfluxPoint> devicePoints = new ArrayList<>();
    Float totalKWHs = null;

    for (GridDeviceDTO device : solarSample.getDevices()) {

      List<Float> deviceInputWatts = new ArrayList<>();
      List<Float> deviceInputVoltages = new ArrayList<>();

      List<Float> deviceOutputWatts = new ArrayList<>();
      List<Float> deviceOutputVoltages = new ArrayList<>();

      List<Float> deviceFrequencies = new ArrayList<>();

      for (GridInputDTO input : device.getInputs()) {
        var point = convertInputDTO(input,device.getId());
        setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
            solarSample.getTimestamp(), systemId);
        res.add(point);

        deviceInputWatts.add(input.getWatt());
        deviceInputVoltages.add(input.getVoltage());
      }

      for (GridOutputDTO output : device.getOutputs()) {
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

      var devicePoint = GridSolarInfluxPoint.builder()
          .totalKWH(device.getTotalKWH())
          .totalOH(device.getTotalOH())
          .deviceTemperature(solarSample.getDeviceTemperature())
          .id(device.getId())
          .build();

      if(device.getChargeWatt() == null){
        devicePoint.setChargeWatt(calculateSum(deviceInputWatts));
        devicePoint.setChargeVoltage(calculateMeanByPercentage(device.getInputs().stream().map(i->new ImmutablePair<Float,Float>(i.getVoltage(),i.getWatt())).collect(Collectors.toList()),devicePoint.getChargeWatt()));
        if(devicePoint.getChargeVoltage() <= 0){
          devicePoint.setChargeAmpere(0.f);
        }else{
          devicePoint.setChargeAmpere(devicePoint.getChargeWatt()/devicePoint.getChargeVoltage());
        }
      }else{
        devicePoint.setChargeWatt(device.getChargeWatt());
        devicePoint.setChargeAmpere(device.getChargeAmpere());
        devicePoint.setChargeVoltage(device.getChargeVoltage());
      }

      if(device.getGridWatt() == null){
        devicePoint.setGridWatt(calculateSum(deviceOutputWatts));
        devicePoint.setGridVoltage(calculateMeanByPercentage(device.getOutputs().stream().map(o->new ImmutablePair<Float,Float>(o.getVoltage(),o.getWatt())).collect(Collectors.toList()),devicePoint.getGridWatt()));
        devicePoint.setGridAmpere(devicePoint.getGridWatt()/devicePoint.getGridVoltage());
      }else{
        devicePoint.setGridWatt(device.getGridWatt());
        devicePoint.setGridAmpere(device.getGridAmpere());
        devicePoint.setGridVoltage(device.getGridVoltage());
      }

      if(devicePoint.getChargeWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate ChargeWatt -> missing 'charge parameters'");
      }
      if(devicePoint.getGridWatt() == null){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate GridWatt -> missing 'grid parameters'");
      }

      if(device.getFrequency() == null){
        devicePoint.setFrequency(calculateMean(deviceFrequencies));
      }else{
        devicePoint.setFrequency(calculateMean(deviceFrequencies));
      }

      setGenericInfluxPointBaseClassAttributes(devicePoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(devicePoint);

      totalKWHs = addWithZeroCheck(totalKWHs,device.getTotalKWH());
      devicePoints.add(devicePoint);
    }

    if(influxPoint.getChargeWatt() == null){
      influxPoint.setChargeWatt(calculateSum(devicePoints.stream().map(GridSolarInfluxPoint::getChargeWatt).collect(Collectors.toList())));
      influxPoint.setChargeVoltage(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getChargeVoltage(),d.getChargeWatt())).collect(Collectors.toList()),influxPoint.getChargeWatt()));
      if(influxPoint.getChargeVoltage() <= 0){
        influxPoint.setChargeAmpere(0.f);
      }else{
        influxPoint.setChargeAmpere(influxPoint.getChargeWatt()/influxPoint.getChargeVoltage());
      }
    }

    if(influxPoint.getGridWatt() == null){
      influxPoint.setGridWatt(calculateSum(devicePoints.stream().map(GridSolarInfluxPoint::getGridWatt).collect(Collectors.toList())));
      influxPoint.setGridVoltage(calculateMeanByPercentage(devicePoints.stream().map(d->new ImmutablePair<Float,Float>(d.getGridVoltage(),d.getGridWatt())).collect(Collectors.toList()),influxPoint.getGridWatt()));
      influxPoint.setGridAmpere(influxPoint.getGridWatt()/influxPoint.getGridVoltage());
    }

    if(influxPoint.getChargeWatt() == null){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate ChargeWatt -> missing 'charge parameters'");
    }
    if(influxPoint.getGridWatt() == null){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"could not calculate GridWatt -> missing 'grid parameters'");
    }

    if(influxPoint.getFrequency() != null){
      influxPoint.setFrequency(calculateMean(devicePoints.stream().map(GridSolarInfluxPoint::getFrequency).collect(Collectors.toList())));
    }

    if(influxPoint.getDeviceTemperature() != null){
      influxPoint.setFrequency(calculateMean(devicePoints.stream().map(GridSolarInfluxPoint::getDeviceTemperature).collect(Collectors.toList())));
    }

    if(influxPoint.getTotalOH() != null){
      influxPoint.setFrequency(calculateMean(devicePoints.stream().map(GridSolarInfluxPoint::getTotalOH).collect(Collectors.toList())));
    }

    if(influxPoint.getTotalKWH() != null){
      influxPoint.setFrequency(totalKWHs);
    }

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    res.add(influxPoint);

    return res;
  }

  @PostMapping("/devices")
  public void PostDevice(@RequestParam long systemId, @RequestBody @Valid DeviceGridSolarSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMulti(systemId,solarSample,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/devices/mult")
  public void PostDeviceMult(@RequestParam long systemId, @RequestBody @Valid List<DeviceGridSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultipleMulti(systemId,solarSamples,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

}
