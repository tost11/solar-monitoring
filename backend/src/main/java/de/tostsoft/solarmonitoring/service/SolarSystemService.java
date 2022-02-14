package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;

import java.util.*;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
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


  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO,User user) {
    if(user == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    ArrayList<String> labels= new ArrayList();
    labels.add("SolarSystem");
    labels.add(registerSolarSystemDTO.getType().toString());

    SolarSystem solarSystem = SolarSystem.builder()
        .name(registerSolarSystemDTO.getName())
        .initialisationFinished(false)
        .latitude(registerSolarSystemDTO.getLatitude())
        .creationDate(Instant.now())
        .longitude(registerSolarSystemDTO.getLongitude())
        .type(registerSolarSystemDTO.getType())
        .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? new Date(registerSolarSystemDTO.getBuildingDate() * 1000L).toInstant() : null)
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

    solarSystem = solarSystemRepository.save(solarSystem);

    return RegisterSolarSystemResponseDTO.builder()
        .id(solarSystem.getId())
        .buildingDate(solarSystem.getBuildingDate()!=null ? Date.from(solarSystem.getBuildingDate()) : null)
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
  }

  public SolarSystemDTO getSystem(long id) {
    SolarSystem solarSystem = solarSystemRepository.findById(id);
    return convertSystemToDTO(solarSystem);
  }

  public List<SolarSystemDTO> getSystems() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<SolarSystem> solarSystems = user.getRelationOwns();
    return solarSystems.stream().map(this::convertSystemToDTO).collect(Collectors.toList());
  }

  public void deleteSystem(long id) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    /*User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem solarSystem = solarSystemRepository.findByToken(token);
    if (solarSystem == null) {
      throw new Exception("System not exist");

    }
    for (SolarSystem ownsSystem: user.getRelationOwns()){
      if (ownsSystem.getToken().equals(solarSystem.getToken())) {
        grafanaService.deleteDashboard(solarSystem.getGrafanaUid());
        solarSystemRepository.deleteByToken(token);

      }
    }

    }*/
  }

  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO) {

    var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
   SolarSystem oldSolarSystem = solarSystemRepository.findById(newSolarSystemDTO.getId()).get();
   User owner = oldSolarSystem.getRelationOwnedBy();
   SolarSystemDTO oldSolarSystemDTO= convertSystemToDTO(oldSolarSystem);
    if(user.getName().equals(owner.getName())&&user.getId().equals(owner.getId())) {
        if(oldSolarSystemDTO.getType().equals(newSolarSystemDTO.getType())==false)

        if(oldSolarSystemDTO.getName().equals(newSolarSystemDTO.getName())==false&&newSolarSystemDTO.getName()!=null)
          oldSolarSystem.setName(newSolarSystemDTO.getName());
        if(oldSolarSystemDTO.getMaxSolarVoltage().equals(newSolarSystemDTO.getMaxSolarVoltage())==false&&newSolarSystemDTO.getMaxSolarVoltage()!=null)
          oldSolarSystem.setMaxSolarVoltage(newSolarSystemDTO.getMaxSolarVoltage());
        if(oldSolarSystemDTO.getIsBatteryPercentage().equals(newSolarSystemDTO.getIsBatteryPercentage())==false&&newSolarSystemDTO.getIsBatteryPercentage()!=null)
          oldSolarSystem.setIsBatteryPercentage(newSolarSystemDTO.getIsBatteryPercentage());
        if(oldSolarSystemDTO.getBatteryVoltage().equals(newSolarSystemDTO.getBatteryVoltage())==false&&newSolarSystemDTO.getBatteryVoltage()!=null)
          oldSolarSystem.setBatteryVoltage(newSolarSystemDTO.getBatteryVoltage());
        if(oldSolarSystemDTO.getInverterVoltage().equals(newSolarSystemDTO.getInverterVoltage())==false&&newSolarSystemDTO.getInverterVoltage()!=null)
          oldSolarSystem.setInverterVoltage(newSolarSystemDTO.getInverterVoltage());
      if(oldSolarSystemDTO.getLongitude().equals(newSolarSystemDTO.getLongitude())==false&&newSolarSystemDTO.getLongitude()!=null)
        oldSolarSystem.setLongitude(newSolarSystemDTO.getLongitude());
      if(oldSolarSystemDTO.getLatitude().equals(newSolarSystemDTO.getLatitude())==false&&newSolarSystemDTO.getLatitude()!=null)
        oldSolarSystem.setLatitude(newSolarSystemDTO.getLatitude());

      solarSystemRepository.save(oldSolarSystem);
      return  convertSystemToDTO(oldSolarSystem);


    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }
}
