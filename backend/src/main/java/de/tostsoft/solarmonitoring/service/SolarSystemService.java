package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
        .build();
  }


  public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO,User user) {
    if(user == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    ArrayList<String> labels= new ArrayList();
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
        .build();

    solarSystemRepository.save(solarSystem);

    solarSystem.setGrafanaId(grafanaService.createNewSelfmadeDeviceSolarDashboard(solarSystem).getId());

    String token = UUID.randomUUID().toString();
    solarSystem.setToken(passwordEncoder.encode(token));
    labels.remove(Neo4jLabels.NOT_FINISHED.toString());
    solarSystem.setLabels(labels);
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

    List<SolarSystem> solarSystems =  solarSystemRepository.findAllByRelationOwnedById(user.getId());

    return solarSystems.stream().map(this::convertSystemToDTO).collect(Collectors.toList());
  }

  public ResponseEntity deleteSystem(SolarSystem solarSystem)  {

          solarSystem.addLabel(Neo4jLabels.IS_DELETED.toString());
          solarSystemRepository.save(solarSystem);
          return ResponseEntity.status(HttpStatus.OK).body("System ist Deleted");
      }

}
