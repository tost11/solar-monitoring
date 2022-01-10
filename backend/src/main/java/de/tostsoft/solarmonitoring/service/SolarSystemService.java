package de.tostsoft.solarmonitoring.service;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private String createGrafanaDashboard(final String bucketName, String token, long userId) {
    var resp = grafanaService.createNewSelfmadeDeviceSolarDashboard(bucketName, token, userId);
    if (resp == null) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "coult not create system");
    }
    return resp.getUid();
  }

  private void checkCreateBucket(final String bucketName) {
    if (influxConnection.doseBucketExit(bucketName)) {
      return;
    }
    var ret = influxConnection.createNewBucket(bucketName);
    if (ret == null) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "coult not create system");
    }
  }


  public SolarSystemDTO add(RegisterSolarSystemDTO registerSolarSystemDTO){
    return add(registerSolarSystemDTO,null,null);
  }

  //TODO rollback changes if one step fails or better write cleanup script
  public SolarSystemDTO add(RegisterSolarSystemDTO registerSolarSystemDTO,final String givenToken,User user)  {
    if(user==null){
      user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    Date creationDate;
    if(registerSolarSystemDTO.getCreationDate()==null){
      creationDate = new Date();
    }
    else{
      creationDate = new Date((long) registerSolarSystemDTO.getCreationDate() * 1000);///TODO second Date
    }


    String token;
    if(givenToken == null) {
      token = UUID.randomUUID().toString();
    }else{
      token = givenToken;
    }

    var dashboardUid = createGrafanaDashboard("generated "+user.getName(),token,user.getGrafanId());

    if (registerSolarSystemDTO.getLatitude() != null && registerSolarSystemDTO.getLongitude() != null) {
        SolarSystem solarSystem = new SolarSystem(token, registerSolarSystemDTO.getName(), creationDate, registerSolarSystemDTO.getType(),dashboardUid);
        solarSystem.setLatitude(registerSolarSystemDTO.getLatitude());
        solarSystem.setLongitude(registerSolarSystemDTO.getLongitude());
    }

    SolarSystem solarSystem = new SolarSystem(token, registerSolarSystemDTO.getName(), creationDate, registerSolarSystemDTO.getType(),dashboardUid);
    solarSystem.setRelationOwnedBy(user);
    solarSystemRepository.save(solarSystem);
    user.addMySystems(solarSystem);

    SolarSystemDTO DTO = new SolarSystemDTO(solarSystem.getName(), solarSystem.getCreationDate().getTime(), solarSystem.getType());
    DTO.setToken(solarSystem.getToken());
    return DTO;
  }

  public ResponseEntity allwaysexist() {
    return ResponseEntity.status(HttpStatus.OK).body("");
  }

  public SolarSystem getSystem(String token) {
    SolarSystem solarSystem = solarSystemRepository.findByToken(token);
    return solarSystem;

  }

  public List<SolarSystemDTO> getSystems() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<SolarSystem> solarSystems = user.getRelationOwns();
    return solarSystems.stream().map((system) -> {
      return new SolarSystemDTO(system.getName(), system.getCreationDate().getTime(), system.getType());
    }).collect(Collectors.toList());


  }

  public User getUserBySystemToken(String token) {
    SolarSystem solarSystem = solarSystemRepository.findByToken(token);
    User userOwn = solarSystem.getRelationOwnedBy();
    return userOwn;

  }

  public void deleteSystem(String token) throws Exception {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem solarSystem = solarSystemRepository.findByToken(token);
    if (solarSystem == null) {
      throw new Exception("System not exist");

    }
    if (user.getRelationOwns().contains(solarSystem)) {
      solarSystemRepository.deleteByToken(token);
    }
  }
}
