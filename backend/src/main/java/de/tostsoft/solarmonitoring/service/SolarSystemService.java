package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.controller.StatusController;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jManages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.ViewData;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.repository.DeletedSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.MyAwesomeSolarSystemSaveRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;

import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
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
  private DeletedSolarSystemRepository deletedSolarSystemRepository;

  @Autowired
  ManagesRepository managesRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private StatusController statusController;

  @Autowired
  private ManagerService managerService;

  //@Autowired
  //private MyAwesomeSolarSystemSaveRepository myAwesomeSolarSystemSaveRepository;

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  public ViewDataDTO convertToViewDataDTO(ViewData viewData){
    return convertToViewDataDTO(viewData,PublicMode.ALL);
  }

  public ViewDataDTO convertToViewDataDTO(ViewData viewData,PublicMode publicMode){
    return ViewDataDTO.builder()
        .isBatteryPercentage(publicMode == PublicMode.ALL ? viewData.getIsBatteryPercentage():null)
        .hasDCOutput(publicMode == PublicMode.ALL && viewData.getHasDCOutput() == Boolean.TRUE)
        .hasACInput(publicMode == PublicMode.ALL && viewData.getHasACInput() == Boolean.TRUE)
        .hasACOutput(publicMode == PublicMode.ALL && viewData.getHasACOutput() == Boolean.TRUE)
        .batteryVoltage(publicMode == PublicMode.ALL ? viewData.getBatteryVoltage() : null)
        .voltageAC(publicMode == PublicMode.ALL ? viewData.getVoltageAC() : null)
        .showAmpere(publicMode == PublicMode.ALL ? null : viewData.getShowAmpere())
        .maxSolarVoltage(viewData.getMaxSolarVoltage())
        .build();
  }

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
        .viewName(solarSystem.getViewName())
        .type(solarSystem.getType())
        .viewData(convertToViewDataDTO(solarSystem.getViewData()))
        .managers(withManagers?managerService.convertListManagesToManagerDTO(solarSystem.getManagedBy()):null)
        .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem, PublicMode publicMode) {
    return SolarSystemDTO.builder()
            .id(solarSystem.getId())
            .buildingDate(solarSystem.getBuildingDate())
            .latitude(solarSystem.getLatitude())
            .longitude(solarSystem.getLongitude())
            .name(solarSystem.getName())
            .type(solarSystem.getType())
            .viewData(convertToViewDataDTO(solarSystem.getViewData(),publicMode))
            .managers(null)
            .timezone(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone())
            .publicMode(solarSystem.getPublicMode())
            .publicFlagOnlyProduction(solarSystem.getPublicMode() == PublicMode.PRODUCTION)
            .build();
  }

  public SolarSystemListItemDTO convertSystemToListItemDTO(SolarSystem neo4jSolarSystem,String role){
    return SolarSystemListItemDTO.builder()
            .id(neo4jSolarSystem.getId())
            .name(neo4jSolarSystem.getViewName())
            .role(role)
            .type(neo4jSolarSystem.getType())
            .build();
  }

  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO,User user) {
    if(user == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(user.getOwns().size() >= user.getNumAllowedSystems()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
    }

    String token = UUID.randomUUID().toString();

    var vd = ViewData.builder()
        .isBatteryPercentage(registerSolarSystemDTO.getIsBatteryPercentage())
        .hasACInput(registerSolarSystemDTO.getHasACInput())
        .hasDCOutput(registerSolarSystemDTO.getHasDCOutput())
        .hasACOutput(registerSolarSystemDTO.getHasACOutput())
        .showAmpere(registerSolarSystemDTO.getShowAmpere())
        .voltageAC(registerSolarSystemDTO.getVoltageAC())
        .batteryVoltage(registerSolarSystemDTO.getBatteryVoltage())
        .maxSolarVoltage(registerSolarSystemDTO.getMaxSolarVoltage())
        .build();

    var objectId = new ObjectId();

    var solarSystem = SolarSystem.builder()
            .id(objectId.toString())
            .influxTagName(objectId.toString())
            .viewName(registerSolarSystemDTO.getName())
            .name(StringUtils.lowerCase(registerSolarSystemDTO.getName()))
            .latitude(registerSolarSystemDTO.getLatitude())
            .creationDate(ZonedDateTime.now())
            .longitude(registerSolarSystemDTO.getLongitude())
            .type(registerSolarSystemDTO.getType())
            .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? ZonedDateTime.ofInstant(registerSolarSystemDTO.getBuildingDate().toInstant(),ZoneId.of(registerSolarSystemDTO.getTimezone())) : null)
            .ownedBy(user)
            .token(passwordEncoder.encode(token))
            .viewData(vd)
            .timezone(registerSolarSystemDTO.getTimezone())
            .publicMode(registerSolarSystemDTO.getPublicMode())
            .build();

    //TODO add viewData object
    return RegisterSolarSystemResponseDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? solarSystem.getBuildingDate() : null)
        .creationDate(solarSystem.getCreationDate())
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .viewName(solarSystem.getViewName())
        .type(solarSystem.getType())
        .token(token)
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return createSystemForUser(registerSolarSystemDTO,user);
  }


  public SolarSystemDTO getSystemWithUserFromContextOrPublic(String id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    Optional<SolarSystem> optSolarSystem = solarSystemRepository.findById(id);
    if(optSolarSystem.isEmpty()){
      return null;//then throws forbidden
    }

    var solarSystem = optSolarSystem.get();

    if(auth != null) {
      var user = (User) auth.getPrincipal();

      var managesOpt = solarSystem.getManagedBy().stream().filter(man -> man.getUser() == user).findAny();
      boolean isOwner = solarSystem.getOwnedBy() == user;

      if (isOwner || managesOpt.isPresent()) {

        var manages = managesOpt.get();

        boolean showMangers = isOwner || manages.getPermission() == Permissions.ADMIN;
        boolean showStatus =
            isOwner || manages.getPermission() == Permissions.ADMIN || manages.getPermission() == Permissions.MANAGE;

        var res = convertSystemToDTO(solarSystem, showMangers);
        if (showStatus) {
          res.setStatus(statusController.getAllStatusInternal(solarSystem));
        }

        return res;
      }
    }

    if(solarSystem.getPublicMode() == PublicMode.NONE){
      return null;
    }

    return convertSystemToDTO(solarSystem, solarSystem.getPublicMode());
  }

  public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
    var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var user = userRepository.findById(authUser.getId()).get();

    ArrayList<SolarSystemListItemDTO> res = new ArrayList<>();

    for (var system : user.getOwns()) {
      res.add(convertSystemToListItemDTO(system, "owns"));
    }

    for (var manages : user.getManges()) {
      res.add(convertSystemToListItemDTO(manages.getSolarSystem(), manages.getPermission().toString()));
    }

    return res;
  }

  public List<SolarSystemListItemDTO> getPublicSystems() {

    List<SolarSystem> solarSystems = solarSystemRepository.findAllByPublicModeIsNot(PublicMode.NONE);

    var auth = SecurityContextHolder.getContext().getAuthentication();
    User user = auth != null && auth.isAuthenticated() ? (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;

    List<SolarSystemListItemDTO> res = new ArrayList<>();

    for (SolarSystem solarSystem : solarSystems) {
      String mode = "public";

      if(user != null){
        if(StringUtils.equals(solarSystem.getOwnedBy().getId(),user.getId())){
          mode = "owns";
        }else if(solarSystem.getManagedBy().stream().anyMatch(man-> man.getUser().equals(user) && man.getPermission() == Permissions.ADMIN || man.getPermission() == Permissions.MANAGE)){
          mode = "owns";//TODO maby change that here
        }else if(solarSystem.getManagedBy().stream().anyMatch(man-> man.getUser().equals(user) && man.getPermission() == Permissions.VIEW)){
          mode = "manages";
        }
      }
      res.add(convertSystemToListItemDTO(solarSystem,mode));
    }
    return res;
  }


  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem){
      deletedSolarSystemRepository.save(solarSystem);
      solarSystemRepository.delete(solarSystem);
      return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
  }

  public SolarSystemDTO patchSolarSystem(PatchSolarSystemDTO newSolarSystemDTO, SolarSystem solarSystem) {

    boolean timeZoneChanged = !StringUtils.equals(newSolarSystemDTO.getTimezone(), solarSystem.getTimezone());

    solarSystem.setName(StringUtils.lowerCase(newSolarSystemDTO.getName()));
    solarSystem.setViewName(newSolarSystemDTO.getName());
    solarSystem.setBuildingDate(newSolarSystemDTO.getBuildingDate());
    solarSystem.setType(newSolarSystemDTO.getType());
    solarSystem.setLatitude(newSolarSystemDTO.getLatitude());
    solarSystem.setLongitude(newSolarSystemDTO.getLongitude());
    solarSystem.getViewData().setShowAmpere(newSolarSystemDTO.getViewData().getShowAmpere());
    solarSystem.getViewData().setIsBatteryPercentage(newSolarSystemDTO.getViewData().getIsBatteryPercentage());
    solarSystem.getViewData().setHasACInput(newSolarSystemDTO.getViewData().getHasACInput());
    solarSystem.getViewData().setHasACOutput(newSolarSystemDTO.getViewData().getHasACOutput());
    solarSystem.getViewData().setHasDCOutput(newSolarSystemDTO.getViewData().getHasDCOutput());
    solarSystem.getViewData().setVoltageAC(newSolarSystemDTO.getViewData().getVoltageAC());
    solarSystem.getViewData().setBatteryVoltage(newSolarSystemDTO.getViewData().getBatteryVoltage());
    solarSystem.getViewData().setMaxSolarVoltage(newSolarSystemDTO.getViewData().getMaxSolarVoltage());
    solarSystem.setTimezone(newSolarSystemDTO.getTimezone());
    solarSystem.setPublicMode(newSolarSystemDTO.getPublicMode());

    var res = solarSystemRepository.save(solarSystem);

    if(timeZoneChanged){
      LOG.info("System timezone changed run full generation of day values");

      if(influxTaskService.runInitial(res)){
        throw new ResponseStatusException(HttpStatus.FORBIDDEN ,"This calculation is only allowed once a day try tomorrow");
      }
    }

    return convertSystemToDTO(res);
  }

  public NewTokenDTO createNewToken(SolarSystem solarSystem) {
    String token = UUID.randomUUID().toString();
    solarSystemRepository.updateToken(solarSystem.getId(),token);
    return new NewTokenDTO(token);
  }

  public SolarSystem findSystemWithOwnedBy(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOptional = solarSystemRepository.findByIdAndOwnedById(systemId,user.getId());
    return solarSystemOptional.get();
  }

  public SolarSystem findSystemWithFullAccess(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOpt = solarSystemRepository.findByIdAndOwnedById(systemId,user.getId());
    if(solarSystemOpt.isPresent()){
      return solarSystemOpt.get();
    }
    var manges = managesRepository.findByUserIdAndSolarSystemIdAndPermissionIn(user.getId(),systemId, List.of(Permissions.ADMIN));
    if(manges != null){
      return manges.getSolarSystem();
    }
    return null;
  }

  public SolarSystem findSystemWithMangeAccess(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOpt = solarSystemRepository.findByIdAndOwnedById(systemId,user.getId());
    if(solarSystemOpt.isPresent()){
      return solarSystemOpt.get();
    }
    var manges = managesRepository.findByUserIdAndSolarSystemIdAndPermissionIn(user.getId(),systemId, Arrays.asList(Permissions.ADMIN,Permissions.MANAGE));
    if(manges != null){
      return manges.getSolarSystem();
    }
    return null;
  }

  public Pair<SolarSystem, PublicMode> findSolarSystemByWithAccess(String systemId){

    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOpt = solarSystemRepository.findByIdAndOwnedById(systemId,user.getId());
    if(solarSystemOpt.isPresent()){
      return new ImmutablePair(solarSystemOpt.get(),null);
    }
    var manges = managesRepository.findByUserIdAndSolarSystemIdAndPermissionIn(user.getId(),systemId, List.of(Permissions.ADMIN));
    if(manges != null){
      return new ImmutablePair(manges.getSolarSystem(),null);
    }

    solarSystemOpt = solarSystemRepository.findByIdAndPublicModeIsNot(systemId,PublicMode.NONE);
    if(solarSystemOpt.isPresent()){
      return new ImmutablePair(solarSystemOpt.get(),solarSystemOpt.get().getPublicMode());
    }

    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no access on this System");
  }
}
