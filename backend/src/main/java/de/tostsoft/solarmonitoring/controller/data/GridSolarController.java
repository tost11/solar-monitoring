package de.tostsoft.solarmonitoring.controller.data;

import static de.tostsoft.solarmonitoring.controller.data.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.DeviceGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.InputOutputGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.SimpleGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridInputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridOutputDTO;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxInputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxOutputPoint;
import de.tostsoft.solarmonitoring.model.grid.GridSolarInfluxPoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
  SolarDataConverter solarDataConverter;


  private void validateAndFillMissing(GridInputDTO solarSample){
    if (solarSample.getWatt() == null) {
      solarSample.setWatt(solarSample.getAmpere() * solarSample.getVoltage());
    }
  }

  private void validateAndFillMissing(GridOutputDTO solarSample){
    if (solarSample.getWatt() == null) {
      solarSample.setWatt(solarSample.getAmpere() * solarSample.getVoltage());
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
    float mult = values.size() / 100.f;
    for (Float value : values) {
      res += res * mult;
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
    if (solarSample.getChargeWatt() == null) {
      solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
    }
    if (solarSample.getGridWatt() == null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
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
  public void PostDataSimple(@RequestParam long systemId, @RequestBody SimpleGridSolarSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("simple/mult")
  public void PostDataSimpleMult(@RequestParam long systemId, @RequestBody List<SimpleGridSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  // ------------------------------------------- input output ----------------------------------------------------------------------------

  private void validateAndFillMissing(InputOutputGridSolarSampleDTO solarSample){
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
    if (solarSample.getChargeWatt() == null && solarSample.getChargeAmpere() != null && solarSample.getChargeVoltage() != null) {
      solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
    }
    if (solarSample.getGridWatt() == null && solarSample.getGridAmpere() != null && solarSample.getGridVoltage() == null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
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
    }

    if(influxPoint.getGridWatt() == null){
      influxPoint.setGridWatt(calculateMean(outputWatts));
      influxPoint.setGridVoltage(calculateMean(outputVoltages));
      influxPoint.setGridAmpere(influxPoint.getGridWatt()/influxPoint.getGridVoltage());
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
  }

  // ---------------------------------------------------- device ------------------------------------------------------

  private void validateAndFillMissing(DeviceGridSolarSampleDTO solarSample){
    if(solarSample.getDuration() <= 0){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
    }

    if(CollectionUtils.isEmpty(solarSample.getDevices())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs empty");
    }

    for (GridDeviceDTO device : solarSample.getDevices()) {

      if(device.getId() <= 0){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
      }

      if(CollectionUtils.isEmpty(device.getInputs())){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs on devices empty");
      }
      if(CollectionUtils.isEmpty(device.getOutputs())){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputs on devices empty");
      }
      if(device.getInputs().stream().anyMatch(v->v.getId() <= 0) ||
          device.getOutputs().stream().anyMatch(v->v.getId() <= 0)){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be greater than zero");
      }
    }

    if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
      solarSample.setTimestamp(new Date().getTime());
    }
    if (solarSample.getChargeWatt() == null && solarSample.getChargeAmpere() != null && solarSample.getChargeVoltage() != null) {
      solarSample.setChargeWatt(solarSample.getChargeAmpere() * solarSample.getChargeVoltage());
    }
    if (solarSample.getGridWatt() == null && solarSample.getGridAmpere() != null && solarSample.getGridVoltage() == null) {
      solarSample.setGridWatt(solarSample.getGridAmpere() * solarSample.getGridVoltage());
    }

    solarSample.getDevices().forEach(d->d.getInputs().forEach(this::validateAndFillMissing));
    solarSample.getDevices().forEach(d->d.getOutputs().forEach(this::validateAndFillMissing));
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

    List<Float> inputWatts = new ArrayList<>();
    List<Float> inputVoltages = new ArrayList<>();

    List<Float> outputWatts = new ArrayList<>();
    List<Float> outputVoltages = new ArrayList<>();

    List<Float> outputFrequencies = new ArrayList<>();
    List<Float> tempartures = new ArrayList<>();
    Float totalKWHs = null;
    List<Float> OHs = new ArrayList<>();

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
          .chargeVoltage(calculateMean(deviceInputWatts))
          .chargeWatt(calculateMean(deviceInputVoltages))
          .gridVoltage(calculateMean(deviceOutputVoltages))
          .gridWatt(calculateMean(deviceOutputWatts))
          .totalKWH(device.getTotalKWH())
          .totalOH(device.getTotalOH())
          .frequency(calculateMean(deviceFrequencies))
          .deviceTemperature(solarSample.getDeviceTemperature())
          .id(device.getId())
          .build();

      devicePoint.setChargeAmpere(devicePoint.getChargeWatt() / devicePoint.getChargeVoltage());
      devicePoint.setGridAmpere(devicePoint.getGridWatt() / devicePoint.getGridVoltage());

      setGenericInfluxPointBaseClassAttributes(devicePoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);
      res.add(devicePoint);

      inputWatts.addAll(deviceInputWatts);
      outputWatts.addAll(deviceOutputWatts);
      inputVoltages.addAll(deviceInputVoltages);
      outputVoltages.addAll(deviceOutputVoltages);
      outputFrequencies.addAll(deviceFrequencies);
      totalKWHs = addWithZeroCheck(totalKWHs,device.getTotalKWH());
      if(device.getTotalOH() != null){
        OHs.add(device.getTotalOH());
      }
      if(device.getDeviceTemperature() != null){
        tempartures.add(device.getDeviceTemperature());
      }
    }

    if(influxPoint.getChargeWatt() == null){
      influxPoint.setChargeWatt(calculateMean(inputWatts));
      influxPoint.setChargeVoltage(calculateMean(inputVoltages));
      influxPoint.setChargeAmpere(influxPoint.getChargeWatt()/influxPoint.getChargeVoltage());
    }

    if(influxPoint.getGridWatt() == null){
      influxPoint.setGridWatt(calculateMean(outputWatts));
      influxPoint.setGridVoltage(calculateMean(outputVoltages));
      influxPoint.setGridAmpere(influxPoint.getGridWatt()/influxPoint.getGridVoltage());
    }

    if(influxPoint.getFrequency() != null && !outputFrequencies.isEmpty()){
      influxPoint.setFrequency(calculateMean(outputFrequencies));
    }

    if(influxPoint.getDeviceTemperature() != null && !tempartures.isEmpty()){
      influxPoint.setFrequency(calculateMean(tempartures));
    }

    if(influxPoint.getTotalOH() != null && !OHs.isEmpty()){
      influxPoint.setFrequency(calculateMean(OHs));
    }

    if(influxPoint.getTotalKWH() != null){
      influxPoint.setFrequency(totalKWHs);
    }

    setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

    res.add(influxPoint);

    return res;
  }

  @PostMapping("/devices")
  public void PostDevice(@RequestParam long systemId, @RequestBody DeviceGridSolarSampleDTO solarSample, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMulti(systemId,solarSample,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

  @PostMapping("/devices/mult")
  public void PostDevice(@RequestParam long systemId, @RequestBody List<DeviceGridSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
    solarDataConverter.genericHandleMultipleMulti(systemId,solarSamples,clientToken,SolarSystemType.GRID,(sample)->{
      validateAndFillMissing(sample);
      return convertToInfluxPoint(sample,systemId);
    });
  }

}
