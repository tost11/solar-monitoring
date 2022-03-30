package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.NewTokenDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.model.ManageBY;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.MyAwesomeSolarSystemSaveRepository;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    //as string or enum

    String token = UUID.randomUUID().toString();

    SolarSystem solarSystem = SolarSystem.builder()
            .name(registerSolarSystemDTO.getName())
            .latitude(registerSolarSystemDTO.getLatitude())
            .creationDate(LocalDateTime.now())
            .longitude(registerSolarSystemDTO.getLongitude())
            .type(registerSolarSystemDTO.getType())
            .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? LocalDateTime.ofInstant(registerSolarSystemDTO.getBuildingDate().toInstant(), ZoneId.systemDefault()) : null)
            .relationOwnedBy(user)
            .labels(labels)
            .token(passwordEncoder.encode(token))
            .isBatteryPercentage(registerSolarSystemDTO.getIsBatteryPercentage())
            .inverterVoltage(registerSolarSystemDTO.getInverterVoltage())
            .batteryVoltage(registerSolarSystemDTO.getBatteryVoltage())
            .maxSolarVoltage(registerSolarSystemDTO.getMaxSolarVoltage())
            .build();

    try {
      solarSystem = myAwesomeSolarSystemSaveRepository.createNewSystem(solarSystem);
    }catch (Exception e){
      LOG.error("Could not save system",e);
      return null;
    }

    return RegisterSolarSystemResponseDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? Date.from(solarSystem.getBuildingDate().atZone(ZoneId.systemDefault()).toInstant()) : null)
        .creationDate(Date.from(solarSystem.getCreationDate().atZone(ZoneId.systemDefault()).toInstant()))
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .type(solarSystem.getType())
        .token(token)
        .build();
  }

  public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return createSystemForUser(registerSolarSystemDTO,user);
  }

  //TODO replace with extra query for view user or something like that
  public SolarSystemDTO getSystemWithUserFromContext(long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnedOrRelationManageWithRelations(id, user.getId());
    if (solarSystem == null) {
      return  null;
    }
    //check if user is permitted to se see and mange editors
    boolean showManagers = solarSystem.getRelationOwnedBy().getId().equals(user.getId()) ||
        solarSystem.getRelationManageBy().stream().anyMatch(u->u.getUser().getId().longValue() == user.getId().longValue() && u.getPermission() == Permissions.ADMIN);
    return convertSystemToDTO(solarSystem,showManagers);
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

  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem){
      solarSystemRepository.addDeleteLabel(solarSystem.getId());
      return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
  }

  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO,SolarSystem solarSystem) {
    SolarSystem res = null;
    try {
      res = myAwesomeSolarSystemSaveRepository.updateSystem(solarSystem,newSolarSystemDTO);
    } catch (Exception e) {
      LOG.error("error on updating system",e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return  convertSystemToDTO(res);
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
