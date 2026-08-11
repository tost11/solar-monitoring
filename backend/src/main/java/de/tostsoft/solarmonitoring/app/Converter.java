package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.AdminTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.CreateTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.NotificationDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserAccessSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CurrentValuesDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.EditSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ManagesSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.MultSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NamingsDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PublicSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SystemInformationsDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewSolarSystemDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.Notification;
import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import org.apache.commons.collections4.CollectionUtils;
import de.tostsoft.solarmonitoring.lib.model.DeviceNamings;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.ViewData;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Converter {

  static public ManagerDTO convertManagesToManagerDTO(Manages manages) {
    return new ManagerDTO(manages.getUser().getId(), manages.getUser().getViewName(), manages.getPermission());
  }

  static public List<ManagerDTO> convertListManagesToManagerDTO(Collection<Manages> manages) {
    return manages.stream().map(Converter::convertManagesToManagerDTO).collect(Collectors.toList());
  }

  static public CurrentValuesDTO converterToCurrentValuesDTO(CurrentValues currentValues){
    return CurrentValuesDTO.builder()
            .inputWatt(currentValues.getInputWatt())
            .outputWatt(currentValues.getOutputWatt())
            .gridWatt(currentValues.getGridWatt())
            .batteryVoltage(currentValues.getBatteryVoltage())
            .batteryPercentage(currentValues.getBatteryPercentage())
            .batteryWatt(currentValues.getBatteryWatt())
            .build();
  }

  static public ViewDataDTO convertToViewDataDTO(ViewData viewData){
      return convertToViewDataDTO(viewData, PublicMode.ALL, true);
  }

  static public ViewDataDTO convertToViewDataDTO(ViewData viewData, PublicMode publicMode, boolean isOwner){
      Set<GraphFilter> filteredGraphFilters = viewData.getGraphFilter();
      Boolean hasTemperature = viewData.getHasTemperature();

      // Filter graph filters based on access level
      if (filteredGraphFilters != null && publicMode == PublicMode.PRODUCTION && !isOwner) {
          // Only production data is public - remove consumption-related filters
          // so consumption graphs appear (even though they won't have data)
          Set<GraphFilter> consumptionFilters = Set.of(
              GraphFilter.OUTPUT_WATT_DC,
              GraphFilter.OUTPUT_WATT_AC,
              GraphFilter.OUTPUT_WATT_COMBINED,
              GraphFilter.OUTPUT_VOLTAGE_DC,
              GraphFilter.OUTPUT_VOLTAGE_AC,
              GraphFilter.OUTPUT_AMPERE_DC,
              GraphFilter.OUTPUT_AMPERE_AC,
              GraphFilter.OUTPUT_FREQUENCY,
              GraphFilter.OUTPUT_TOTAL_CONSUMPTION,
              GraphFilter.BATTERY_WATT,
              GraphFilter.BATTERY_VOLTAGE,
              GraphFilter.BATTERY_AMPERE,
              GraphFilter.BATTERY_SOC,
              GraphFilter.GRID_WATT,
              GraphFilter.GRID_VOLTAGE,
              GraphFilter.GRID_AMPERE,
              GraphFilter.GRID_FREQUENCY,
              GraphFilter.MORE_TEMPERATURE
          );

          // Remove consumption filters from existing filters
          filteredGraphFilters = new HashSet<>(filteredGraphFilters);
          filteredGraphFilters.removeAll(consumptionFilters);

          // Hide temperature accordion for public viewers (temperature is private data)
          hasTemperature = false;
      }

      // Filter grid graphs based on showGridInfo flag (applies to all users)
      if (filteredGraphFilters != null && viewData.getShowGridInfo() != null && !viewData.getShowGridInfo()) {
          Set<GraphFilter> gridFilters = Set.of(
              GraphFilter.GRID_WATT,
              GraphFilter.GRID_VOLTAGE,
              GraphFilter.GRID_AMPERE,
              GraphFilter.GRID_FREQUENCY
          );
          filteredGraphFilters = new HashSet<>(filteredGraphFilters);
          filteredGraphFilters.removeAll(gridFilters);
      }

      // Filter temperature based on hasTemperature flag (applies to all users, not just public)
      if (filteredGraphFilters != null && hasTemperature != null && !hasTemperature) {
          filteredGraphFilters = new HashSet<>(filteredGraphFilters);
          filteredGraphFilters.remove(GraphFilter.MORE_TEMPERATURE);
      }

      return ViewDataDTO.builder()
          .batteryVoltage(viewData.getBatteryVoltage())
          .maxSolarVoltage(viewData.getMaxSolarVoltage())
          .hasTemperature(hasTemperature)
          .productionForTotalPricing(viewData.getProductionForTotalPricing())
          .totalPricingPublicOverride(viewData.getTotalPricingPublicOverride())
          .hideTotalConsumption(viewData.getHideTotalConsumption())
          .showGridInfo(viewData.getShowGridInfo())
          .defaultDelay(viewData.getDefaultDelay())
          .totalFilter(viewData.getTotalFilter())
          .graphFilter(filteredGraphFilters)
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
            .batteryVoltage(viewData.getBatteryVoltage())
            .maxSolarVoltage(viewData.getMaxSolarVoltage())
            .hasTemperature(orElse(viewData.getHasTemperature(),false))
            .productionForTotalPricing(viewData.getProductionForTotalPricing())
            .totalPricingPublicOverride(viewData.getTotalPricingPublicOverride())
            .hideTotalConsumption(viewData.getHideTotalConsumption())
            .showGridInfo(orElse(viewData.getShowGridInfo(), false))
            .defaultDelay(viewData.getDefaultDelay())
            .totalFilter(viewData.getTotalFilter())
            .graphFilter(viewData.getGraphFilter())
            .build();
  }


  static public SystemInformationsDTO convertToSystemInformationsDTO(SystemInformations info, boolean isPublic) {
    if (info == null) {
      return null;
    }

    return SystemInformationsDTO.builder()
        .name(isPublic && info.getPublicName() != null ? info.getPublicName() : info.getViewName())
        .publicName(isPublic ? null : info.getPublicName())
        .description(info.getDescription())
        .maxInstalledSolarPower(info.getMaxInstalledSolarPower())
        .maxInverterOutputPower(isPublic ? null : info.getMaxInverterOutputPower())
        .batteryCapacity(isPublic ? null : info.getBatteryCapacity())
        .buildingDate(info.getBuildingDate() != null ? ZonedDateTime.of(info.getBuildingDate(), ZoneId.systemDefault()) : null)
        .electricityPrice(isPublic ? null : info.getElectricityPrice())
        .electricityPriceFeedIn(isPublic ? null : info.getElectricityPriceFeedIn())
        .build();
  }

  static public SystemInformations convertToSystemInformations(SystemInformationsDTO dto, de.tostsoft.solarmonitoring.app.util.HtmlSanitizer htmlSanitizer) {
    if (dto == null) {
      return new SystemInformations();
    }

    return SystemInformations.builder()
        .name(StringUtils.lowerCase(dto.getName()))
        .viewName(dto.getName())
        .publicName(dto.getPublicName())
        .description(htmlSanitizer.sanitizeDescription(dto.getDescription()))
        .maxInstalledSolarPower(dto.getMaxInstalledSolarPower())
        .maxInverterOutputPower(dto.getMaxInverterOutputPower())
        .batteryCapacity(dto.getBatteryCapacity())
        //TODO find out if UTC convertion here is needed
        .buildingDate(dto.getBuildingDate() != null ? dto.getBuildingDate().toLocalDateTime() : null)
        .electricityPrice(dto.getElectricityPrice())
        .electricityPriceFeedIn(dto.getElectricityPriceFeedIn())
        .build();
  }

  static public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem) {
    SystemInformations info = solarSystem.getSystemInformations();
    return SolarSystemDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(info.getBuildingDate() != null ? info.getBuildingDate().atZone(ZoneId.of(solarSystem.getTimezone())) : null)
        .creationDate(solarSystem.getCreationDate().atZone(ZoneId.of(solarSystem.getTimezone())))
        .name(solarSystem.getSystemInformations().getViewName())
        .shortener(solarSystem.getShortener())
        .description(info.getDescription())
        .type(solarSystem.getType())
        .viewData(convertToViewDataDTO(solarSystem.getViewData()))
        .managers(convertListManagesToManagerDTO(solarSystem.getManagedBy()))
        .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
        .publicMode(solarSystem.getPublicMode())
        .namings(convertNamingsToDTO(solarSystem.getNamings()))
        .electricityPrice(info.getElectricityPrice() )
        .electricityPriceFeedIn(info.getElectricityPriceFeedIn())
        .maxInstalledSolarPower(info.getMaxInstalledSolarPower())
        .maxInverterOutputPower(info.getMaxInverterOutputPower())
        .batteryCapacity(info.getBatteryCapacity())
        .deyeSunSerialNumbers(Converter.convertDeyeSerialsToString(solarSystem.getDeyeSunSerials()))
        .calculateCombinedValuesAfterwards(solarSystem.getCalculateCombinedValuesAfterwards())
        .tags(solarSystem.getTags() == null ? new ArrayList<>() : solarSystem.getTags().stream().map(Converter::convertTagToTagDTO).collect(Collectors.toList()))
        .build();
  }

  static public PublicSolarSystemDTO convertSystemToPublicDTO(SolarSystem solarSystem) {
    SystemInformations info = solarSystem.getSystemInformations();
    String displayName = info.getPublicName() != null ? info.getPublicName() : info.getViewName();
    return PublicSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .buildingDate(info.getBuildingDate() != null ? info.getBuildingDate().atZone(ZoneId.of(solarSystem.getTimezone())) : null)
            .creationDate(solarSystem.getCreationDate().atZone(ZoneId.of(solarSystem.getTimezone())))
            .shortener(solarSystem.getShortener())
            .name(displayName)
            .description(info.getDescription())
            .type(solarSystem.getType())
            .viewData(convertToViewDataDTO(solarSystem.getViewData(), solarSystem.getPublicMode(), false))
            .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
            .publicMode(solarSystem.getPublicMode())
            .namings(convertNamingsToDTO(solarSystem.getNamings()))
            .maxInstalledSolarPower(info.getMaxInstalledSolarPower())
            .tags(solarSystem.getTags() == null ? new ArrayList<>() : solarSystem.getTags().stream().map(Converter::convertTagToTagDTO).collect(Collectors.toList()))
            .build();
  }

  static public ManagesSolarSystemDTO convertSystemToManagerDTO(SolarSystem solarSystem) {
    SystemInformations info = solarSystem.getSystemInformations();
    return ManagesSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .buildingDate(info.getBuildingDate() != null ? info.getBuildingDate().atZone(ZoneId.of(solarSystem.getTimezone())) : null)
            .creationDate(solarSystem.getCreationDate().atZone(ZoneId.of(solarSystem.getTimezone())))
            .name(info.getViewName())
            .shortener(solarSystem.getShortener())
            .description(info.getDescription())
            .type(solarSystem.getType())
            .viewData(convertToViewDataDTO(solarSystem.getViewData()))
            .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
            .publicMode(solarSystem.getPublicMode())
            .namings(convertNamingsToDTO(solarSystem.getNamings()))
            .electricityPrice(info.getElectricityPrice())
            .electricityPriceFeedIn(info.getElectricityPriceFeedIn())
            .maxInstalledSolarPower(info.getMaxInstalledSolarPower())
            .maxInverterOutputPower(info.getMaxInverterOutputPower())
            .batteryCapacity(info.getBatteryCapacity())
            .deyeSunSerialNumbers(Converter.convertDeyeSerialsToString(solarSystem.getDeyeSunSerials()))
            .calculateCombinedValuesAfterwards(solarSystem.getCalculateCombinedValuesAfterwards())
            .tags(solarSystem.getTags() == null ? new ArrayList<>() : solarSystem.getTags().stream().map(Converter::convertTagToTagDTO).collect(Collectors.toList()))
            .build();
  }

  static public ViewSolarSystemDTO convertSystemToViewDTO(SolarSystem solarSystem) {
    SystemInformations info = solarSystem.getSystemInformations();
    return ViewSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .buildingDate(info.getBuildingDate() != null ? info.getBuildingDate().atZone(ZoneId.of(solarSystem.getTimezone())) : null)
            .creationDate(solarSystem.getCreationDate().atZone(ZoneId.of(solarSystem.getTimezone())))
            .shortener(solarSystem.getShortener())
            .name( info.getViewName())
            .description(info.getDescription())
            .type(solarSystem.getType())
            .viewData(convertToViewDataDTO(solarSystem.getViewData()))
            .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
            .publicMode(solarSystem.getPublicMode())
            .namings(convertNamingsToDTO(solarSystem.getNamings()))
            .electricityPrice(info.getElectricityPrice())
            .electricityPriceFeedIn(info.getElectricityPriceFeedIn())
            .maxInstalledSolarPower(info.getMaxInstalledSolarPower())
            .maxInverterOutputPower(info.getMaxInverterOutputPower())
            .batteryCapacity(info.getBatteryCapacity())
            .tags(solarSystem.getTags() == null ? new ArrayList<>() : solarSystem.getTags().stream().map(Converter::convertTagToTagDTO).collect(Collectors.toList()))
            .build();
  }

  static public NotificationDTO converterToNotificationDTO(Notification notification) {
    return NotificationDTO.builder()
            .id(notification.getId())
            .value(notification.getValue())
            .type(notification.getType())
            .solarSystemId(notification.getSolarSystem().getId())
            .solarSystemName(notification.getSolarSystem().getSystemInformations().getViewName())
            .solarSystemType(notification.getSolarSystem().getType())
            .build();
  }

  static public Notification converterToNotification(NotificationDTO notification) {
    return Notification.builder()
            .value(notification.getValue())
            .type(notification.getType())
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

  static public Set<Long> convertStringToDeyeSerials(String serials){
    if(serials == null){
      return null;
    }
    var deyeSerials = new HashSet<Long>();
    for (String serialString : StringUtils.split(serials, ",")) {
      deyeSerials.add(Long.parseLong(serialString));
    }
    return deyeSerials;
  }

  static public String convertDeyeSerialsToString(Set<Long> serials){
    if(serials == null){
      return null;
    }
    return StringUtils.joinWith(",",serials.stream().map(Object::toString).toArray());
  }

  static public NamingsDTO convertNamingsToDTO(Map<Long,DeviceNamings> naming){
    var ret = NamingsDTO.builder()
        .batteries(new HashMap<>())
        .devices(new HashMap<>())
        .inputsDC(new HashMap<>())
        .inputsAC(new HashMap<>())
        .outputsDC(new HashMap<>())
        .outputsAC(new HashMap<>())
        .grids(new HashMap<>())
        .build();

    if(naming != null){

      for(Entry<Long, DeviceNamings> namingEntry : naming.entrySet()) {
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
        for (Entry<Integer, String> e : saveMap(namingEntry.getValue().getGrids()).entrySet()) {
          ret.getGrids().put(""+namingEntry.getKey()+"-"+e.getKey(),e.getValue());
        }
      }
    }

    return ret;
  }

  public static UserAccessSystemDTO converterSystemToUserAccessSystem(SolarSystem sys) {
    return UserAccessSystemDTO.builder()
            .id(sys.getId())
            .name(sys.getSystemInformations().getViewName())
            .type(sys.getType())
            .build();
  }

  public static EditSolarSystemDTO convertSystemToEditResponseDTO(SolarSystem solarSystem) {
    return EditSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .shortener(solarSystem.getShortener())
            .type(solarSystem.getType())
            .timezone(solarSystem.getTimezone())
            .publicMode(solarSystem.getPublicMode())
            .calculateCombinedValuesAfterwards(solarSystem.getCalculateCombinedValuesAfterwards())
            .viewData(convertToViewDataDTO(solarSystem.getViewData()))
            .namings(convertNamingsToDTO(solarSystem.getNamings()))
            .deyeSunSerialNumbers(convertDeyeSerialsToString(solarSystem.getDeyeSunSerials()))
            .systemInformations(convertToSystemInformationsDTO(solarSystem.getSystemInformations(), false))
            .tags(solarSystem.getTags() == null ? new ArrayList<>() : solarSystem.getTags().stream().map(Converter::convertTagToTagDTO).collect(Collectors.toList()))
            .build();
  }

  private interface AddInterface{
    void add(int id,String name,DeviceNamings deviceNamings);
  }

  static private void addToNamingInputOutputBatteryMap(Entry<String, String>entry, Map<Long,DeviceNamings> deviceMap,AddInterface inter){
    var arr = StringUtils.split(entry.getKey(),"-");
    long deviceId = Long.parseLong(arr[0]);
    int id = Integer.parseInt(arr[1]);

    var deviceNaming = deviceMap.get(deviceId);
    if(deviceNaming == null){
      deviceMap.put(deviceId, new DeviceNamings(""));
      deviceNaming = deviceMap.get(deviceId);
    }

    inter.add(id,entry.getValue(),deviceNaming);
  }

  static public Map<Long,DeviceNamings> convertDTOtoNamings(NamingsDTO naming){

    var res = new HashMap<Long,DeviceNamings>();

    if(naming != null){

      if(naming.getDevices() != null) {
          for (Entry<String, String> device : naming.getDevices().entrySet()) {
              res.put(Long.parseLong(device.getKey()), new DeviceNamings(device.getValue()));
          }
      }

      if(naming.getInputsDC() != null) {
          for (Entry<String, String> e : naming.getInputsDC().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getInputsDC().put(id, name));
          }
      }

      if(naming.getOutputsAC() != null) {
          for (Entry<String, String> e : naming.getInputsAC().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getInputsAC().put(id, name));
          }
      }

      if(naming.getOutputsDC() != null) {
          for (Entry<String, String> e : naming.getOutputsDC().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getOutputsDC().put(id, name));
          }
      }

      if(naming.getOutputsAC() != null) {
          for (Entry<String, String> e : naming.getOutputsAC().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getOutputsAC().put(id, name));
          }
      }

      if(naming.getBatteries() != null) {
          for (Entry<String, String> e : naming.getBatteries().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getBatteries().put(id, name));
          }
      }

      if(naming.getGrids() != null) {
          for (Entry<String, String> e : naming.getGrids().entrySet()) {
              addToNamingInputOutputBatteryMap(e, res, (id, name, dm) -> dm.getGrids().put(id, name));
          }
      }
    }
    return res;
  }

  static public SolarSystemListItemDTO convertSystemToListItemDTO(SolarSystem solarSystem,String role){
    return SolarSystemListItemDTO.builder()
        .id(solarSystem.getId())
        .name(solarSystem.getSystemInformations().getViewName())
        .role(role)
        .type(solarSystem.getType())
        .shortener(solarSystem.getShortener())
        .build();
  }

  static public MultSolarSystemDTO convertSystemToMultSolarSystemDTO(SolarSystem solarSystem){
    return MultSolarSystemDTO.builder()
            .id(solarSystem.getId())
            .name(solarSystem.getSystemInformations().getName())
            .viewName(solarSystem.getSystemInformations().getViewName())
            .type(solarSystem.getType())
            .publicMode(solarSystem.getPublicMode())
            .shortner(solarSystem.getShortener())
            .defaultDuration(solarSystem.getViewData() != null ? solarSystem.getViewData().getDefaultDelay():null)
            .build();
  }

  static public List<MultSolarSystemDTO> convertSystemsToMultSolarSystemDTOs(Collection<SolarSystem> manages) {
    return manages.stream().map(Converter::convertSystemToMultSolarSystemDTO).collect(Collectors.toList());
  }

  static public UserDTO converterUserToUserDTO(User user){
    return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .isAdmin(user.getIsAdmin())
            .mail(user.getMail())
            .numAllowedSystems(user.getNumAllowedSystems())
            .notifications(CollectionUtils.emptyIfNull(user.getNotifications()).stream().map(Converter::converterToNotificationDTO).collect(Collectors.toList()))
            .accessSystems(new ArrayList<>())
            .build();
  }

  static public Tag convertCreateTagDTOtoTag(CreateTagDTO tagDTO){
    return Tag.builder()
            .viewName(tagDTO.getName())
            .id(tagDTO.getId())
            .name(StringUtils.lowerCase(tagDTO.getName()))
            .locked(tagDTO.getLocked())
            .color(tagDTO.getColor())
            .showOnStartPage(tagDTO.getShowOnStartPage())
            .showStartPageAggregation(tagDTO.getShowStartPageAggregation())
            .build();
  }

  static public TagDTO convertTagToTagDTO(Tag tag){
    return TagDTO.builder()
            .id(tag.getId())
            .name(tag.getViewName())
            .color(tag.getColor())
            .build();
  }

  static public AdminTagDTO convertTagToTAdminTagDTO(Tag tag){
    return AdminTagDTO.builder()
            .id(tag.getId())
            .name(tag.getViewName())
            .color(tag.getColor())
            .locked(tag.getLocked())
            .showOnStartPage(tag.getShowOnStartPage())
            .showStartPageAggregation(tag.getShowStartPageAggregation())
            .build();
  }

}
