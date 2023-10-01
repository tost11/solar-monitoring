package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.model.*;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class Converter {

  static public ManagerDTO convertManagesToManagerDTO(Manages manages) {
    return new ManagerDTO(manages.getUser().getId(), manages.getUser().getViewName(), manages.getPermission());
  }

  static public List<ManagerDTO> convertListManagesToManagerDTO(Collection<Manages> manages) {
    return manages.stream().map(Converter::convertManagesToManagerDTO).collect(Collectors.toList());
  }

  static public ViewDataDTO convertToViewDataDTO(ViewData viewData){
      return ViewDataDTO.builder()
          .isBatteryPercentage(viewData.getIsBatteryPercentage())
          .hasDCOutput(viewData.getHasDCOutput())
          .hasACInput(viewData.getHasACInput())
          .hasACOutput(viewData.getHasACOutput())
          .batteryVoltage(viewData.getBatteryVoltage())
          .voltageAC(viewData.getVoltageAC())
          .showAmpere(viewData.getShowAmpere())
          .maxSolarVoltage(viewData.getMaxSolarVoltage())
          .hasTemperature(viewData.getHasTemperature())
          .productionForTotalPricing(viewData.getProductionForTotalPricing())
          .build();
  }


  static private <T> T orElse(T toCheck,T or){
    if(toCheck == null){
      return or;
    }
    return toCheck;
  }

  static public ViewData convertToViewData(ViewDataDTO viewData){
    return ViewData.builder()
            .isBatteryPercentage(orElse(viewData.getIsBatteryPercentage(),false))
            .hasDCOutput(orElse(viewData.getHasDCOutput(),false))
            .hasACInput(orElse(viewData.getHasACInput(),false))
            .hasACOutput(orElse(viewData.getHasACOutput(),false))
            .batteryVoltage(viewData.getBatteryVoltage())
            .voltageAC(viewData.getVoltageAC())
            .showAmpere(orElse(viewData.getShowAmpere(),false))
            .maxSolarVoltage(viewData.getMaxSolarVoltage())
            .hasTemperature(orElse(viewData.getHasTemperature(),false))
            .productionForTotalPricing(viewData.getProductionForTotalPricing())
            .build();
  }


  static public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem,boolean withManagers) {
    return SolarSystemDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate() != null ? solarSystem.getBuildingDate().atZone(ZoneId.of(solarSystem.getTimezone())) : null)
        .creationDate(solarSystem.getCreationDate().atZone(ZoneId.of(solarSystem.getTimezone())))
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .shortener(solarSystem.getShortener())
        .viewName(solarSystem.getViewName())
        .type(solarSystem.getType())
        .viewData(convertToViewDataDTO(solarSystem.getViewData()))
        .managers(withManagers?convertListManagesToManagerDTO(solarSystem.getManagedBy()):null)
        .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
        .publicMode(solarSystem.getPublicMode())
        .namings(convertNamingsToDTO(solarSystem.getNamings()))
        .electricityPrice(withManagers ? solarSystem.getElectricityPrice() : null)
        .build();
  }

  /*static public TotalValuesDTO convertTotalValuesToTotalValuesDTO(TotalValues totalValues,boolean allValues){
    if(totalValues == null){
      return null;
    }
    return TotalValuesDTO.builder()
            .calcProducedKWH(totalValues.getCalcProducedKWH())
            .producedKWH(totalValues.getProducedKWH())
            .consumedKWH(allValues ? totalValues.getConsumedKWH() :  null)
            .calcConsumedKWH(allValues ? totalValues.getCalcConsumedKWH(): null)
            .earnedMoney(allValues ? totalValues.getEarnedMoney(): null)
            .build();
  }*/

  static private Map<Integer,String> saveMap(HashMap<Integer,String>map){
    if(map == null){
      return new HashMap<>();
    }
    return map;
  }

  static public NamingsDTO convertNamingsToDTO(Map<Integer,DeviceNamings> naming){
    var ret = NamingsDTO.builder()
        .batteries(new HashMap<>())
        .devices(new HashMap<>())
        .inputsDC(new HashMap<>())
        .inputsAC(new HashMap<>())
        .outputsDC(new HashMap<>())
        .outputsAC(new HashMap<>())
        .build();

    if(naming != null){

      for(Entry<Integer, DeviceNamings> namingEntry : naming.entrySet()) {
        if(!StringUtils.isBlank(namingEntry.getValue().getName())){
          ret.getDevices().put(""+namingEntry.getKey(),namingEntry.getValue().getName());
        }

        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getInputsDC()).entrySet()) {
          ret.getInputsDC().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }

        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getInputsAC()).entrySet()) {
          ret.getInputsAC().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }
        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getOutputsDC()).entrySet()) {
          ret.getOutputsDC().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }
        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getOutputsAC()).entrySet()) {
          ret.getOutputsAC().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }
        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getBatteries()).entrySet()) {
          ret.getBatteries().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }
      }
    }

    return ret;
  }

  private interface AddInterface{
    void add(int id,String name,DeviceNamings deviceNamings);
  }

  static private void addToNamingInputOutputBatteryMap(Entry<String, String>entry, Map<Integer,DeviceNamings> deviceMap,AddInterface inter){
    var arr = StringUtils.split(entry.getKey(),"-");
    int deviceId = Integer.parseInt(arr[0]);
    int id = Integer.parseInt(arr[1]);

    var deviceNaming = deviceMap.get(deviceId);
    if(deviceNaming == null){
      deviceMap.put(deviceId, new DeviceNamings(""));
      deviceNaming = deviceMap.get(deviceId);
    }

    inter.add(id,entry.getValue(),deviceNaming);
  }

  static public Map<Integer,DeviceNamings> convertDTOtoNamings(NamingsDTO naming){

    var res = new HashMap<Integer,DeviceNamings>();

    if(naming != null){

      for (Entry<String, String> device : naming.getDevices().entrySet()) {
        res.put(Integer.parseInt(device.getKey()), new DeviceNamings(device.getValue()));
      }

      for (Entry<String, String> e : naming.getInputsDC().entrySet()) {
        addToNamingInputOutputBatteryMap(e,res,(id,name,dm)->dm.getInputsDC().put(id,name));
      }

      for (Entry<String, String> e : naming.getInputsAC().entrySet()) {
        addToNamingInputOutputBatteryMap(e,res,(id,name,dm)->dm.getInputsAC().put(id,name));
      }

      for (Entry<String, String> e : naming.getOutputsDC().entrySet()) {
        addToNamingInputOutputBatteryMap(e,res,(id,name,dm)->dm.getOutputsDC().put(id,name));
      }

      for (Entry<String, String> e : naming.getOutputsAC().entrySet()) {
        addToNamingInputOutputBatteryMap(e,res,(id,name,dm)->dm.getOutputsAC().put(id,name));
      }

      for (Entry<String, String> e : naming.getBatteries().entrySet()) {
        addToNamingInputOutputBatteryMap(e,res,(id,name,dm)->dm.getBatteries().put(id,name));
      }
    }
    return res;
  }

  static public SolarSystemListItemDTO convertSystemToListItemDTO(SolarSystem solarSystem,String role){
    return SolarSystemListItemDTO.builder()
        .id(solarSystem.getId())
        .name(solarSystem.getViewName())
        .role(role)
        .type(solarSystem.getType())
        .shortener(solarSystem.getShortener())
        .build();
  }

  static public MultSolarSystemDTO convertSystemToMultSolarSystemDTO(SolarSystem solarSystem){
    return MultSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .name(solarSystem.getName())
            .viewName(solarSystem.getViewName())
            .type(solarSystem.getType())
            .publicMode(solarSystem.getPublicMode())
            .shortner(solarSystem.getShortener())
            .build();
  }

  static public List<MultSolarSystemDTO> convertSystemsToMultSolarSystemDTOs(Collection<SolarSystem> manages) {
    return manages.stream().map(Converter::convertSystemToMultSolarSystemDTO).collect(Collectors.toList());
  }

}
