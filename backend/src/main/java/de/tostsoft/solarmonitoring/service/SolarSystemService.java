package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.Converter;
import de.tostsoft.solarmonitoring.controller.StatusController;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.ViewData;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;

import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
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
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private StatusController statusController;

  private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO, User user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (user.getOwns().size() >= user.getNumAllowedSystems()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have to much Systems");
    }

    String token = UUID.randomUUID().toString();

    var vd = Converter.convertToViewData(registerSolarSystemDTO.getViewData());

    var objectId = new ObjectId();

    var solarSystem = SolarSystem.builder()
            .id(objectId.toString())
            .influxTagName(objectId.toString())
            .viewName(registerSolarSystemDTO.getName())
            .name(StringUtils.lowerCase(registerSolarSystemDTO.getName()))
            .latitude(registerSolarSystemDTO.getLatitude())
            .creationDate(LocalDateTime.now())
            .longitude(registerSolarSystemDTO.getLongitude())
            .type(registerSolarSystemDTO.getType())
            .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? registerSolarSystemDTO.getBuildingDate().toLocalDateTime() : null)
            .ownedBy(user)
            .token(passwordEncoder.encode(token))
            .viewData(vd)
            .timezone(registerSolarSystemDTO.getTimezone())
            .publicMode(registerSolarSystemDTO.getPublicMode())
            .namings(Converter.convertDTOtoNamings(registerSolarSystemDTO.getNamings()))
            .build();

    solarSystem = solarSystemRepository.save(solarSystem);

    return RegisterSolarSystemResponseDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? ZonedDateTime.of(solarSystem.getBuildingDate(),ZoneId.of(solarSystem.getTimezone())) : null)
        .creationDate(ZonedDateTime.of(solarSystem.getCreationDate(),ZoneId.of(solarSystem.getTimezone())))
        .latitude(solarSystem.getLatitude())
        .longitude(solarSystem.getLongitude())
        .name(solarSystem.getName())
        .viewName(solarSystem.getViewName())
        .type(solarSystem.getType())
        .token(token)
        .viewData(Converter.convertToViewDataDTO(solarSystem.getViewData()))
        .namings(Converter.convertNamingsToDTO(solarSystem.getNamings()))
        .publicMode(solarSystem.getPublicMode())
        .build();
  }

  public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return createSystemForUser(registerSolarSystemDTO, user);
  }


  public SolarSystemDTO getSystemWithUserFromContextOrPublic(String id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    Optional<SolarSystem> optSolarSystem = solarSystemRepository.findById(id);
    if (optSolarSystem.isEmpty()) {
      return null;//then throws forbidden
    }

    var solarSystem = optSolarSystem.get();

    if (auth != null) {
      var user = (User) auth.getPrincipal();

      var managesOpt = solarSystem.getManagedBy().stream().filter(man -> man.getUser() != user).findAny();
      boolean isOwner = solarSystem.getOwnedBy().equals(user);

      if (isOwner || managesOpt.isPresent()) {

        boolean showMangers = isOwner || managesOpt.get().getPermission() == Permissions.ADMIN;
        boolean showStatus = isOwner || managesOpt.get().getPermission() == Permissions.ADMIN || managesOpt.get().getPermission() == Permissions.MANAGE;

        var res = Converter.convertSystemToDTO(solarSystem, showMangers);
        if (showStatus) {
          res.setStatus(statusController.getAllStatusInternal(solarSystem));
        }

        return res;
      }
    }

    if (solarSystem.getPublicMode() == PublicMode.NONE) {
      return null;
    }

    var onlyProduction = solarSystem.getPublicMode() == PublicMode.PRODUCTION;

    var res = Converter.convertSystemToDTO(solarSystem,false);

    if(onlyProduction){
      res.setPublicFlagOnlyProduction(true);
      res.setViewData(ViewDataDTO.builder()
          .showAmpere(true)
          .hasTemperature(false)
          .maxSolarVoltage(solarSystem.getViewData().getMaxSolarVoltage())
          .build());
      res.getNamings().getBatteries().clear();
      res.getNamings().getInputsAC().clear();
      res.getNamings().getOutputsDC().clear();
      res.getNamings().getOutputsAC().clear();
    }

    return res;
  }

  public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
    var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var user = userRepository.findById(authUser.getId()).get();

    ArrayList<SolarSystemListItemDTO> res = new ArrayList<>();

    for (var system : user.getOwns()) {
      res.add(Converter.convertSystemToListItemDTO(system, "owns"));
    }

    for (var manages : user.getManges()) {
      res.add(Converter.convertSystemToListItemDTO(manages.getSolarSystem(), manages.getPermission().toString()));
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

      if (user != null) {
        if (StringUtils.equals(solarSystem.getOwnedBy().getId(), user.getId())) {
          mode = "owns";
        } else if (solarSystem.getManagedBy().stream().anyMatch(man -> man.getUser().equals(user) && man.getPermission() == Permissions.ADMIN || man.getPermission() == Permissions.MANAGE)) {
          mode = "owns";//TODO maybe change that here
        } else if (solarSystem.getManagedBy().stream().anyMatch(man -> man.getUser().equals(user) && man.getPermission() == Permissions.VIEW)) {
          mode = "manages";
        }
      }
      res.add(Converter.convertSystemToListItemDTO(solarSystem, mode));
    }
    return res;
  }

  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem) {
    solarSystem.setDeletedAt(LocalDateTime.now());
    solarSystemRepository.save(solarSystem);
    return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
  }

  public SolarSystemDTO patchSolarSystem(PatchSolarSystemDTO newSolarSystemDTO, SolarSystem solarSystem) {

    boolean timeZoneChanged = !StringUtils.equals(newSolarSystemDTO.getTimezone(), solarSystem.getTimezone());

    solarSystem.setName(StringUtils.lowerCase(newSolarSystemDTO.getName()));
    solarSystem.setViewName(newSolarSystemDTO.getName());
    solarSystem.setBuildingDate(newSolarSystemDTO.getBuildingDate() != null ? newSolarSystemDTO.getBuildingDate().toLocalDateTime() : null);
    solarSystem.setType(newSolarSystemDTO.getType());
    solarSystem.setLatitude(newSolarSystemDTO.getLatitude());
    solarSystem.setLongitude(newSolarSystemDTO.getLongitude());

    var vd = Converter.convertToViewData(newSolarSystemDTO.getViewData());
    solarSystem.setViewData(vd);

    solarSystem.setTimezone(newSolarSystemDTO.getTimezone());
    solarSystem.setPublicMode(newSolarSystemDTO.getPublicMode());
    solarSystem.setNamings(Converter.convertDTOtoNamings(newSolarSystemDTO.getNamings()));

    var res = solarSystemRepository.save(solarSystem);

    if (timeZoneChanged) {
      LOG.info("System timezone changed run full generation of day values");

      if (influxTaskService.runInitial(res)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This calculation is only allowed once a day try tomorrow");
      }
    }

    return Converter.convertSystemToDTO(res,false);
  }

  public NewTokenDTO createNewToken(SolarSystem solarSystem) {
    String token = UUID.randomUUID().toString();
    solarSystemRepository.updateToken(solarSystem.getId(),passwordEncoder.encode(token));
    return new NewTokenDTO(token);
  }

  public SolarSystem findSystemWithOwnedBy(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOptional = solarSystemRepository.findByIdAndOwnedById(systemId, user.getId());
    return solarSystemOptional.get();
  }

  public SolarSystem findSystemWithFullAccess(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOpt = solarSystemRepository.findById(systemId);
    if (solarSystemOpt.isEmpty()) {
      return null;
    }
    var system = solarSystemOpt.get();
    if (system.getOwnedBy().equals(user)) {
      return system;
    }
    if (system.getManagedBy().stream()
            .anyMatch(m -> m.getPermission() == Permissions.ADMIN && m.getUser().equals(user))) {
      return system;
    }
    return null;
  }

  public SolarSystem findSystemWithMangeAccess(String systemId) {
    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var solarSystemOpt = solarSystemRepository.findById(systemId);
    if (solarSystemOpt.isEmpty()) {
      return null;
    }
    var system = solarSystemOpt.get();
    if (system.getOwnedBy().equals(user)) {
      return system;
    }
    if (system.getManagedBy().stream()
            .anyMatch(m -> (m.getPermission() == Permissions.ADMIN || m.getPermission() == Permissions.MANAGE) && m.getUser().equals(user))) {
      return system;
    }
    return null;
  }

  private Pair<SolarSystem, PublicMode> sysemtToAccesPair(SolarSystem system) {

    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      var user = (User) auth.getPrincipal();
      if (system.getOwnedBy().equals(user)) {
        return new ImmutablePair(system, null);
      }

      if (system.getManagedBy().stream()
              .anyMatch(m -> m.getUser().equals(user))) {
        return new ImmutablePair(system, null);
      }
    }

    if (system.getPublicMode() == PublicMode.PRODUCTION || system.getPublicMode() == PublicMode.ALL) {
      return new ImmutablePair(system, system.getPublicMode());
    }
    return null;
  }

  public Pair<SolarSystem, PublicMode> findSolarSystemByWithAccess(String systemId) {

    var solarSystemOpt = solarSystemRepository.findById(systemId);
    if (solarSystemOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
    }

    var pair = sysemtToAccesPair(solarSystemOpt.get());
    if (pair == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
    }
    return pair;
  }

  public List<Pair<SolarSystem, PublicMode>> findSolarSystemsByWithAccess(Collection<String> systemIds) {

    var systems = solarSystemRepository.findAllByIdIn(systemIds);
    if (systems.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on any of the System (or they dose not exist)");
    }

    var ret = new ArrayList<Pair<SolarSystem, PublicMode>>();

    for (SolarSystem system : systems) {
      var pair = sysemtToAccesPair(system);
      if (pair != null) {
        ret.add(pair);
      }
    }

    if (ret.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on any of the System (or they dose not exist)");
    }

    return ret;
  }
}