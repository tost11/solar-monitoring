package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.controller.StatusController;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jManages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.repository.MyAwesomeSolarSystemSaveRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class SolarSystemService {

  @Autowired
  private InfluxTaskService influxTaskService;

  @Autowired
  private Neo4jSolarSystemRepository neo4jSolarSystemRepository;

  @Autowired
  private Neo4jUserRepository neo4jUserRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private StatusController statusController;

  @Autowired
  private MyAwesomeSolarSystemSaveRepository myAwesomeSolarSystemSaveRepository;

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  public SolarSystemDTO convertSystemToDTO(Neo4jSolarSystem neo4jSolarSystem){
    return convertSystemToDTO(neo4jSolarSystem,false);
  }

  public SolarSystemDTO convertSystemToDTO(Neo4jSolarSystem neo4jSolarSystem,boolean withManagers) {
    return SolarSystemDTO.builder()
        .id(neo4jSolarSystem.getId())
        .buildingDate(neo4jSolarSystem.getBuildingDate())
        .creationDate(neo4jSolarSystem.getCreationDate())
        .latitude(neo4jSolarSystem.getLatitude())
        .longitude(neo4jSolarSystem.getLongitude())
        .name(neo4jSolarSystem.getName())
        .type(neo4jSolarSystem.getType())
        .isBatteryPercentage(neo4jSolarSystem.getIsBatteryPercentage())
        .hasDCOutput(neo4jSolarSystem.getHasDCOutput())
        .hasACInput(neo4jSolarSystem.getHasACInput())
        .hasACOutput(neo4jSolarSystem.getHasACOutput())
        .batteryVoltage(neo4jSolarSystem.getBatteryVoltage())
        .voltageAC(neo4jSolarSystem.getVoltageAC())
        .showAmpere(neo4jSolarSystem.getShowAmpere())
        .maxSolarVoltage(neo4jSolarSystem.getMaxSolarVoltage())
        .managers(withManagers?convertToManagerDTO(neo4jSolarSystem.getRelationNeo4jManageBy()):null)
        .timezone(neo4jSolarSystem.getTimezone() == null ? "UTC" : neo4jSolarSystem.getTimezone())
        .publicMode(neo4jSolarSystem.getPublicMode())
        .build();
  }

  public SolarSystemDTO convertSystemToDTO(Neo4jSolarSystem neo4jSolarSystem, PublicMode publicMode) {
    return SolarSystemDTO.builder()
            .id(neo4jSolarSystem.getId())
            .buildingDate(neo4jSolarSystem.getBuildingDate())
            .latitude(neo4jSolarSystem.getLatitude())
            .longitude(neo4jSolarSystem.getLongitude())
            .name(neo4jSolarSystem.getName())
            .type(neo4jSolarSystem.getType())
            .isBatteryPercentage(publicMode == PublicMode.ALL ? neo4jSolarSystem.getIsBatteryPercentage():null)
            .hasDCOutput(publicMode == PublicMode.ALL && neo4jSolarSystem.getHasDCOutput() == Boolean.TRUE)
            .hasACInput(publicMode == PublicMode.ALL && neo4jSolarSystem.getHasACInput() == Boolean.TRUE)
            .hasACOutput(publicMode == PublicMode.ALL && neo4jSolarSystem.getHasACOutput() == Boolean.TRUE)
            .batteryVoltage(publicMode == PublicMode.ALL ? neo4jSolarSystem.getBatteryVoltage() : null)
            .voltageAC(publicMode == PublicMode.ALL ? neo4jSolarSystem.getVoltageAC() : null)
            .showAmpere(publicMode == PublicMode.ALL ? null : neo4jSolarSystem.getShowAmpere())
            .maxSolarVoltage(neo4jSolarSystem.getMaxSolarVoltage())
            .managers(null)
            .timezone(neo4jSolarSystem.getTimezone() == null ? "UTC" : neo4jSolarSystem.getTimezone())
            .publicMode(neo4jSolarSystem.getPublicMode())
            .publicFlagOnlyProduction(neo4jSolarSystem.getPublicMode() == PublicMode.PRODUCTION)
            .build();
  }

  private List<ManagerDTO> convertToManagerDTO(List<Neo4jManageBy> neo4jManageBy) {
    return neo4jManageBy.stream().map(
        m -> new ManagerDTO(m.getNeo4jUser().getId(), m.getNeo4jUser().getName(), m.getPermission())).collect(Collectors.toList());
  }

  public SolarSystemListItemDTO convertSystemToListItemDTO(Neo4jSolarSystem neo4jSolarSystem,String role){
    return SolarSystemListItemDTO.builder()
            .id(neo4jSolarSystem.getId())
            .name(neo4jSolarSystem.getName())
            .role(role)
            .type(neo4jSolarSystem.getType())
            .build();
  }

  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO,
      Neo4jUser neo4jUser) {
    if(neo4jUser == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(neo4jUserRepository.countByRelationOwns(neo4jUser.getId()) >= neo4jUser.getNumAllowedSystems()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
    }

    Set<String> labels = new HashSet();
    labels.add(Neo4jLabels.SolarSystem.toString());
    labels.add(registerSolarSystemDTO.getType().toString());

    String token = UUID.randomUUID().toString();

    Neo4jSolarSystem neo4jSolarSystem = Neo4jSolarSystem.builder()
            .name(registerSolarSystemDTO.getName())
            .latitude(registerSolarSystemDTO.getLatitude())
            .creationDate(ZonedDateTime.now())
            .longitude(registerSolarSystemDTO.getLongitude())
            .type(registerSolarSystemDTO.getType())
            .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? ZonedDateTime.ofInstant(registerSolarSystemDTO.getBuildingDate().toInstant(),ZoneId.of(registerSolarSystemDTO.getTimezone())) : null)
            .relationOwnedBy(neo4jUser)
            .labels(labels)
            .token(passwordEncoder.encode(token))
            .isBatteryPercentage(registerSolarSystemDTO.getIsBatteryPercentage())
            .voltageAC(registerSolarSystemDTO.getVoltageAC())
            .hasACInput(registerSolarSystemDTO.getHasACInput())
            .hasACOutput(registerSolarSystemDTO.getHasACOutput())
            .hasDCOutput(registerSolarSystemDTO.getHasDCOutput())
            .batteryVoltage(registerSolarSystemDTO.getBatteryVoltage())
            .maxSolarVoltage(registerSolarSystemDTO.getMaxSolarVoltage())
            .showAmpere(registerSolarSystemDTO.getShowAmpere())
            .timezone(registerSolarSystemDTO.getTimezone())
            .publicMode(registerSolarSystemDTO.getPublicMode())
            .build();

    try {
      neo4jSolarSystem = myAwesomeSolarSystemSaveRepository.createNewSystem(neo4jSolarSystem);
    }catch (Exception e){
      LOG.error("Could not save system",e);
      return null;
    }

    return RegisterSolarSystemResponseDTO.builder()
        .id(neo4jSolarSystem.getId())
        .buildingDate(neo4jSolarSystem.getBuildingDate()!=null ? neo4jSolarSystem.getBuildingDate() : null)
        .creationDate(neo4jSolarSystem.getCreationDate())
        .latitude(neo4jSolarSystem.getLatitude())
        .longitude(neo4jSolarSystem.getLongitude())
        .name(neo4jSolarSystem.getName())
        .type(neo4jSolarSystem.getType())
        .token(token)
        .publicMode(neo4jSolarSystem.getPublicMode())
        .build();
  }

  public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
    var user = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return createSystemForUser(registerSolarSystemDTO,user);
  }


  public SolarSystemDTO getSystemWithUserFromContextOrPublic(long id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if(auth != null){
      Neo4jUser neo4jUser = (Neo4jUser) auth.getPrincipal();
      Neo4jSolarSystem neo4jSolarSystem = neo4jSolarSystemRepository.findByIdAndRelationOwnedOrRelationManageWithRelations(id, neo4jUser.getId());
      if (neo4jSolarSystem != null) {
        //check if user is permitted to se see and mange editors
        boolean showManagers = neo4jSolarSystem.getRelationOwnedBy().getId().equals(neo4jUser.getId()) ||
                neo4jSolarSystem.getRelationNeo4jManageBy().stream().anyMatch(u -> u.getNeo4jUser().getId().longValue() == neo4jUser.getId().longValue() && u.getPermission() == Permissions.ADMIN);

        var res = convertSystemToDTO(neo4jSolarSystem, showManagers);
        if(neo4jSolarSystem.getRelationOwnedBy().getId().equals(neo4jUser.getId()) ||
                neo4jSolarSystem.getRelationNeo4jManageBy().stream().anyMatch(u -> u.getNeo4jUser().getId().longValue() == neo4jUser.getId().longValue() && (u.getPermission() == Permissions.MANAGE || u.getPermission() == Permissions.ADMIN))){//add status information
          res.setStatus(statusController.getAllStatusInternal(neo4jSolarSystem));
        }
        return res;
      }
    }

    Neo4jSolarSystem neo4jSolarSystem = neo4jSolarSystemRepository.getPublicSystemsById(id);
    if(neo4jSolarSystem == null){
      return null;
    }

    return convertSystemToDTO(neo4jSolarSystem, neo4jSolarSystem.getPublicMode());
  }

  public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var fullUser = neo4jUserRepository.findByIdAndLoadRelationsNotDeleted(neo4jUser.getId());
    ArrayList<SolarSystemListItemDTO> collect = new ArrayList<>();
      for (Neo4jSolarSystem system : fullUser.getRelationOwns()) {
          collect.add(convertSystemToListItemDTO(system, "owns"));
    }
    for (Neo4jManages system : fullUser.getRelationManageBy()) {
      collect.add(convertSystemToListItemDTO(system.getNeo4jSolarSystem(), system.getPermission().toString()));
    }

    return collect;
  }

  public List<SolarSystemListItemDTO> getPublicSystems() {

    List<Neo4jSolarSystem> neo4jSolarSystems = neo4jSolarSystemRepository.gitPublicSystems();
    var ret = neo4jSolarSystems.stream().map((v)->convertSystemToListItemDTO(v,"public")).collect(Collectors.toList());

    var auth = SecurityContextHolder.getContext().getAuthentication();

    if(auth != null && auth.isAuthenticated()){
      Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      var fullUser = neo4jUserRepository.findByIdAndLoadRelationsNotDeleted(neo4jUser.getId());
      for (SolarSystemListItemDTO solarSystemDTO : ret) {
        if(neo4jUser.getRelationManageBy().stream().anyMatch((f)-> Objects.equals(f.getId(), solarSystemDTO.getId()))){
          solarSystemDTO.setRole("manages");
        }
        if(neo4jUser.getRelationManageBy().stream().anyMatch((f)-> Objects.equals(f.getId(), solarSystemDTO.getId()))){
          solarSystemDTO.setRole("owns");
        }
      }
    }
    return ret;
  }


  public ResponseEntity<String> deleteSystem(Neo4jSolarSystem neo4jSolarSystem){
      neo4jSolarSystemRepository.addDeleteLabel(neo4jSolarSystem.getId());
      return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
  }

  public SolarSystemDTO patchSolarSystem(PatchSolarSystemDTO newSolarSystemDTO, Neo4jSolarSystem neo4jSolarSystem) {
    Neo4jSolarSystem res;

    boolean timeZoneChanged = !StringUtils.equals(newSolarSystemDTO.getTimezone(), neo4jSolarSystem.getTimezone());

    try {
      res = myAwesomeSolarSystemSaveRepository.updateSystem(neo4jSolarSystem,newSolarSystemDTO);
    } catch (Exception e) {
      LOG.error("error on updating system",e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if(timeZoneChanged){
      LOG.info("System timezone changed run full generation of day values");
      //set data afterwards because calculation needs this data
      res.setRelationOwnedBy(neo4jUserRepository.findByOwnerSystemId(res.getId()));

      if(influxTaskService.runInitial(res)){
        throw new ResponseStatusException(HttpStatus.FORBIDDEN ,"This calculation is only allowed once a day try tomorrow");
      }
    }

    return convertSystemToDTO(res);
  }

  public NewTokenDTO createNewToken(Neo4jSolarSystem neo4jSolarSystem) {
    String token = UUID.randomUUID().toString();
    try{
      myAwesomeSolarSystemSaveRepository.updateSystemWithProp(neo4jSolarSystem,"token",passwordEncoder.encode(token));
    } catch (Exception e) {
      LOG.error("error on updating system",e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
   }
    return new NewTokenDTO(token);
  }

  public Neo4jSolarSystem findSystemWithFullAccess(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdmin(systemId, neo4jUser.getId());
  }

  public Neo4jSolarSystem findSystemWithFullAccessWithAllRelations(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(systemId, neo4jUser.getId());
  }

  public Neo4jSolarSystem findSystemWithFullAccessWithOwner(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminWithOwner(systemId, neo4jUser.getId());
  }

  public Neo4jSolarSystem findSystemWithManageAccess(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(systemId,
        neo4jUser.getId());
  }

  public Neo4jSolarSystem findSystemWithManageAccessWithAllRelations(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMangeWithRelations(systemId, neo4jUser.getId());
  }

  public Neo4jSolarSystem findSystemWithManageAccessWithOwner(long systemId) {
    Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return neo4jSolarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMangeWithOwner(systemId,
        neo4jUser.getId());
  }



}
