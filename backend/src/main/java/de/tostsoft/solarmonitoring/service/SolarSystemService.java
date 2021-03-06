package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.model.ManageBY;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.MyAwesomeSolarSystemSaveRepository;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;

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
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private MyAwesomeSolarSystemSaveRepository myAwesomeSolarSystemSaveRepository;

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem){
    return convertSystemToDTO(solarSystem,false);
  }

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem,boolean withManagers) {
    return SolarSystemDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate())
        .creationDate(solarSystem.getCreationDate())
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .type(solarSystem.getType())
        .isBatteryPercentage(solarSystem.getIsBatteryPercentage())
        .batteryVoltage(solarSystem.getBatteryVoltage())
        .inverterVoltage(solarSystem.getInverterVoltage())
        .maxSolarVoltage(solarSystem.getMaxSolarVoltage())
        .managers(withManagers?convertToManagerDTO(solarSystem.getRelationManageBy()):null)
        .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  private List<ManagerDTO> convertToManagerDTO(List<ManageBY> manageBy) {
    return manageBy.stream().map(manageBY -> new ManagerDTO(manageBY.getUser().getId(),manageBY.getUser().getName(),manageBY.getPermission())).collect(Collectors.toList());
  }

  public SolarSystemListItemDTO convertSystemToListItemDTO(SolarSystem solarSystem,String role){
    return SolarSystemListItemDTO.builder()
            .id(solarSystem.getId())
            .name(solarSystem.getName())
            .role(role)
            .type(solarSystem.getType())
            .build();
  }

  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO,User user) {
    if(user == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(userRepository.countByRelationOwns(user.getId()) >= user.getNumAllowedSystems()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
    }

    Set<String> labels = new HashSet();
    labels.add(Neo4jLabels.SolarSystem.toString());
    labels.add(registerSolarSystemDTO.getType().toString());

    String token = UUID.randomUUID().toString();

    SolarSystem solarSystem = SolarSystem.builder()
            .name(registerSolarSystemDTO.getName())
            .latitude(registerSolarSystemDTO.getLatitude())
            .creationDate(ZonedDateTime.now())
            .longitude(registerSolarSystemDTO.getLongitude())
            .type(registerSolarSystemDTO.getType())
            .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? ZonedDateTime.ofInstant(registerSolarSystemDTO.getBuildingDate().toInstant(),ZoneId.of(registerSolarSystemDTO.getTimezone())) : null)
            .relationOwnedBy(user)
            .labels(labels)
            .token(passwordEncoder.encode(token))
            .isBatteryPercentage(registerSolarSystemDTO.getIsBatteryPercentage())
            .inverterVoltage(registerSolarSystemDTO.getInverterVoltage())
            .batteryVoltage(registerSolarSystemDTO.getBatteryVoltage())
            .maxSolarVoltage(registerSolarSystemDTO.getMaxSolarVoltage())
            .timezone(registerSolarSystemDTO.getTimezone())
            .publicMode(registerSolarSystemDTO.getPublicMode())
            .build();

    try {
      solarSystem = myAwesomeSolarSystemSaveRepository.createNewSystem(solarSystem);
    }catch (Exception e){
      LOG.error("Could not save system",e);
      return null;
    }

    return RegisterSolarSystemResponseDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? solarSystem.getBuildingDate() : null)
        .creationDate(solarSystem.getCreationDate())
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .type(solarSystem.getType())
        .token(token)
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return createSystemForUser(registerSolarSystemDTO,user);
  }


  public SolarSystemDTO getSystemWithUserFromContextOrPublic(long id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if(auth != null){
      User user = (User) auth.getPrincipal();
      SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnedOrRelationManageWithRelations(id, user.getId());
      if (solarSystem != null) {
        //check if user is permitted to se see and mange editors
        boolean showManagers = solarSystem.getRelationOwnedBy().getId().equals(user.getId()) ||
                solarSystem.getRelationManageBy().stream().anyMatch(u -> u.getUser().getId().longValue() == user.getId().longValue() && u.getPermission() == Permissions.ADMIN);
        return convertSystemToDTO(solarSystem, showManagers);
      }
    }

    SolarSystem solarSystem = solarSystemRepository.gitPublicSystemsById(id);
    if(solarSystem == null){
      return null;
    }

    return convertSystemToDTO(solarSystem, false);
  }

  public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var fullUser = userRepository.findByIdAndLoadRelationsNotDeleted(user.getId());
    ArrayList<SolarSystemListItemDTO> collect = new ArrayList<>();
      for (SolarSystem system : fullUser.getRelationOwns()) {
          collect.add(convertSystemToListItemDTO(system, "owns"));
    }
    for (Manages system : fullUser.getRelationManageBy()) {
      collect.add(convertSystemToListItemDTO(system.getSolarSystem(), system.getPermission().toString()));
    }

    return collect;
  }

  public List<SolarSystemListItemDTO> getPublicSystems() {

    List<SolarSystem> solarSystems = solarSystemRepository.gitPublicSystems();
    var ret = solarSystems.stream().map((v)->convertSystemToListItemDTO(v,"public")).collect(Collectors.toList());

    var auth = SecurityContextHolder.getContext().getAuthentication();

    if(auth != null && auth.isAuthenticated()){
      User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      var fullUser = userRepository.findByIdAndLoadRelationsNotDeleted(user.getId());
      for (SolarSystemListItemDTO solarSystemDTO : ret) {
        if(user.getRelationManageBy().stream().anyMatch((f)-> Objects.equals(f.getId(), solarSystemDTO.getId()))){
          solarSystemDTO.setRole("manages");
        }
        if(user.getRelationManageBy().stream().anyMatch((f)-> Objects.equals(f.getId(), solarSystemDTO.getId()))){
          solarSystemDTO.setRole("owns");
        }
      }
    }
    return ret;
  }


  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem){
      solarSystemRepository.addDeleteLabel(solarSystem.getId());
      return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
  }

  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO,SolarSystem solarSystem) {
    SolarSystem res = null;

    boolean timeZoneChanged = !StringUtils.equals(newSolarSystemDTO.getTimezone(),solarSystem.getTimezone());

    try {
      res = myAwesomeSolarSystemSaveRepository.updateSystem(solarSystem,newSolarSystemDTO);
    } catch (Exception e) {
      LOG.error("error on updating system",e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if(timeZoneChanged){
      LOG.info("System timezone changed run full generation of day values");
      res.setRelationOwnedBy(userRepository.findByOwnerSystemId(res.getId()));
      influxTaskService.runInitial(res);
    }

    return convertSystemToDTO(res);
  }

  public NewTokenDTO createNewToken(SolarSystem solarSystem) {
    String token = UUID.randomUUID().toString();
    try{
      myAwesomeSolarSystemSaveRepository.updateSystemWithProp(solarSystem,"token",passwordEncoder.encode(token));
    } catch (Exception e) {
      LOG.error("error on updating system",e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
   }
    return new NewTokenDTO(token);
  }

  public SolarSystem findSystemWithFullAccess(long systemId,boolean loadRelations) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if(loadRelations) {
      return solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(systemId, user.getId());
    }
    return solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdmin(systemId,user.getId());
  }
}
