package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.GettingSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
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

  private String createGrafanaDashboard(final String bucketName,SolarSystemDTO solarSystemDTO,String folderUid,String dashboardUid){
    var resp = grafanaService.createNewSelfmadeDeviceSolarDashboard(bucketName,solarSystemDTO,folderUid,dashboardUid);
    if(resp == null){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"coult not create system");
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
    return add(registerSolarSystemDTO,null,null,null);
  }

  //TODO rollback changes if one step fails or better write cleanup script
  public SolarSystemDTO add(RegisterSolarSystemDTO registerSolarSystemDTO,final String givenToken,User user,final String givenDashboardUid)  {
    if(solarSystemRepository.existsByName(registerSolarSystemDTO.getName())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User Already exist");
    }
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
    SolarSystemDTO solarSystemDTO= new SolarSystemDTO(registerSolarSystemDTO.getName(), creationDate.getTime(),registerSolarSystemDTO.getType());
    solarSystemDTO.setToken(token);

    var dashboardUid = createGrafanaDashboard("generated "+user.getName(),solarSystemDTO,user.getGrafanaFolderUid(),givenDashboardUid);

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
    DTO.setId(solarSystem.getId());
    return DTO;
  }

  public ResponseEntity allwaysexist() {
    return ResponseEntity.status(HttpStatus.OK).body("");
  }

  public GettingSolarSystemDTO getSystem(long id ) {
    SolarSystem solarSystem = solarSystemRepository.findById(id);
    GettingSolarSystemDTO gettingSolarSystemDTO = new GettingSolarSystemDTO(solarSystem.getId(),solarSystem.getToken(),solarSystem.getName(),solarSystem.getCreationDate().getTime(),solarSystem.getType(),solarSystem.getGrafanaUid());
    return gettingSolarSystemDTO;

  }

  public List<GettingSolarSystemDTO> getSystems() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<SolarSystem> solarSystems = user.getRelationOwns();
    return solarSystems.stream().map((system) -> {
      return new GettingSolarSystemDTO(system.getId(),system.getToken(),system.getName(), system.getCreationDate().getTime(), system.getType(),system.getGrafanaUid());
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
    for (SolarSystem ownsSystem: user.getRelationOwns()){
      if (ownsSystem.getToken().equals(solarSystem.getToken())) {
        grafanaService.deleteDashboard(solarSystem.getGrafanaUid());
        solarSystemRepository.deleteByToken(token);

      }
    }
  }
}
