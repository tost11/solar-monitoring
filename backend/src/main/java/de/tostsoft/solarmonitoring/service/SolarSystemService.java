package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.*;
import de.tostsoft.solarmonitoring.model.*;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.Instant;
import java.util.*;

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
  private UserService userService;
  @Autowired
  private MigrationService migrationService;
  @Autowired
  private SolarSystemRepository solarSystemRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private GrafanaService grafanaService;
  @Autowired
  private InfluxConnection influxConnection;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem) {
    return SolarSystemDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? Date.from(solarSystem.getBuildingDate()) : null)
        .creationDate(Date.from(solarSystem.getCreationDate()))
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .type(solarSystem.getType())
        .isBatteryPercentage(solarSystem.getIsBatteryPercentage())
        .batteryVoltage(solarSystem.getBatteryVoltage())
        .inverterVoltage(solarSystem.getInverterVoltage())
        .maxSolarVoltage(solarSystem.getMaxSolarVoltage())
        .build();
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
    if(user.getNumAllowedSystems()<userRepository.countByRelationOwns(user.getId()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");

      Set<String> labels = new HashSet();
      labels.add(Neo4jLabels.SolarSystem.toString());
      labels.add(Neo4jLabels.NOT_FINISHED.toString());
      labels.add(registerSolarSystemDTO.getType().toString());
      //as string or enum

      SolarSystem solarSystem = SolarSystem.builder()
              .name(registerSolarSystemDTO.getName())
              .latitude(registerSolarSystemDTO.getLatitude())
              .creationDate(Instant.now())
              .longitude(registerSolarSystemDTO.getLongitude())
              .type(registerSolarSystemDTO.getType())
              .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? registerSolarSystemDTO.getBuildingDate().toInstant() : null)
              .relationOwnedBy(user)
              .labels(labels)
              .isBatteryPercentage(registerSolarSystemDTO.getIsBatteryPercentage())
              .inverterVoltage(registerSolarSystemDTO.getInverterVoltage())
              .batteryVoltage(registerSolarSystemDTO.getBatteryVoltage())
              .maxSolarVoltage(registerSolarSystemDTO.getMaxSolarVoltage())
              .build();

      solarSystemRepository.save(solarSystem);

      solarSystem.setGrafanaId(grafanaService.createNewSelfmadeDeviceSolarDashboard(solarSystem).getId());

      String token = UUID.randomUUID().toString();
      solarSystem.setToken(passwordEncoder.encode(token));
      labels.remove(Neo4jLabels.NOT_FINISHED.toString());
      solarSystem.setLabels(labels);
      //

      solarSystem = solarSystemRepository.save(solarSystem);

      return RegisterSolarSystemResponseDTO.builder()
              .id(solarSystem.getId())
              .buildingDate(solarSystem.getBuildingDate() != null ? Date.from(solarSystem.getBuildingDate()) : null)
              .creationDate(Date.from(solarSystem.getCreationDate()))
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

     // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
  }

  public SolarSystemDTO getSystem(long id) {
    SolarSystem solarSystem = solarSystemRepository.findById(id);
    return convertSystemToDTO(solarSystem);
  }

  public List<SolarSystemListItemDTO> getSystems() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var fullUser=userRepository.findAllById(user.getId());
    ArrayList<SolarSystemListItemDTO> collect = new ArrayList<>();
      for (SolarSystem system : fullUser.getRelationOwns()) {
          collect.add(convertSystemToListItemDTO(system, "owns"));
    }
    for (Manages system : fullUser.getRelationManageBy()) {
      collect.add(convertSystemToListItemDTO(system.getSolarSystem(), system.getPermissions().toString()));
    }

    return collect;
  }

  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem)  {
        User user = solarSystem.getRelationOwnedBy();
      //  user.setNumberOfSystemy(user.getNumberOfSystemy()-1);
        solarSystem.addLabel(Neo4jLabels.IS_DELETED.toString());
        solarSystemRepository.save(solarSystem);
        return ResponseEntity.status(HttpStatus.OK).body("System ist Deleted");
    }


  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO) {

    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem oldSolarSystem = solarSystemRepository.findById(newSolarSystemDTO.getId()).get();

      oldSolarSystem.setName(newSolarSystemDTO.getName());
      oldSolarSystem.setMaxSolarVoltage(newSolarSystemDTO.getMaxSolarVoltage());
      oldSolarSystem.setIsBatteryPercentage(newSolarSystemDTO.getIsBatteryPercentage());
      oldSolarSystem.setBatteryVoltage(newSolarSystemDTO.getBatteryVoltage());
      oldSolarSystem.setInverterVoltage(newSolarSystemDTO.getInverterVoltage());
      oldSolarSystem.setLongitude(newSolarSystemDTO.getLongitude());
      oldSolarSystem.setLatitude(newSolarSystemDTO.getLatitude());

      solarSystemRepository.save(oldSolarSystem);
      return  convertSystemToDTO(oldSolarSystem);


  }

  public SolarSystemDTO addManageUser(String userName,long solarSystemDTO,Permissions permissions) {

    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem system= solarSystemRepository.findByIdAndRelationOwnedById(solarSystemDTO,user.getId());
    if(system!=null){
      User manager = userRepository.findAllByNameLike(userName);
      boolean isAlsoManager=false;
      for (Manages manageSystem:manager.getRelationManageBy()){
          if(manageSystem.getSolarSystem().getId().equals(system.getId())){
            isAlsoManager=true;
            var m =solarSystemRepository.findByIdAndLoadManegeBy(system.getId());
            if(!m.getRelationManageBy().get(0).getPermissions().equals(permissions)){
             manageSystem.setPermissions(permissions);
             userRepository.save(manager);
             return convertSystemToDTO(system);
            }

          }

      }
      if(isAlsoManager){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"is also Manager");
      }
      Manages manages = new Manages(system,permissions);
      manager.addManages(manages);
      userRepository.save(manager);

      //userRepository.findByNameIgnoreCase(userName);


    }
    SolarSystem s = solarSystemRepository.findByIdAndLoadManegeBy(system.getId());
    System.out.println(s.getRelationManageBy().get(0).getPermissions());

    return  convertSystemToDTO(system);
  }

  public List<ManagerDTO> getManagers(SolarSystem system) {
    ArrayList<ManagerDTO> managers=new ArrayList<>();
    for(ManageBY manageBy: system.getRelationManageBy()){
      managers.add(new ManagerDTO(manageBy.getUser().getId(),manageBy.getUser().getName(),manageBy.getPermissions()));
    }

    return managers;
  }
}
