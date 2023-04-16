package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.ViewData;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import java.util.List;
import java.util.stream.Collectors;

public class Converter {

  static public ManagerDTO convertManagesToManagerDTO(Manages manages) {
    return new ManagerDTO(manages.getUser().getId(), manages.getUser().getViewName(), manages.getPermission());
  }

  static public List<ManagerDTO> convertListManagesToManagerDTO(List<Manages> manages) {
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
          .build();
  }

  static public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem){
    return convertSystemToDTO(solarSystem,false);
  }

  static public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem,boolean withManagers) {
    return SolarSystemDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate())
        .creationDate(solarSystem.getCreationDate())
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .viewName(solarSystem.getViewName())
        .type(solarSystem.getType())
        .viewData(convertToViewDataDTO(solarSystem.getViewData()))
        .managers(withManagers?convertListManagesToManagerDTO(solarSystem.getManagedBy()):null)
        .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  static public SolarSystemListItemDTO convertSystemToListItemDTO(SolarSystem neo4jSolarSystem,String role){
    return SolarSystemListItemDTO.builder()
        .id(neo4jSolarSystem.getId())
        .name(neo4jSolarSystem.getViewName())
        .role(role)
        .type(neo4jSolarSystem.getType())
        .build();
  }
}
