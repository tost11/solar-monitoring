package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
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

@Service
public class SolarSystemService {

  @Autowired
  private UserService userService;
  @Autowired
  private SolarSystemRepository solarSystemRepository;
  @Autowired
  private UserRepository userRepository;


  public SolarSystemDTO add(SolarSystemDTO solarSystemDTO) {
      Date creationDate = new Date((long) solarSystemDTO.getCreationDate() * 1000);
      solarSystemDTO.setToken(UUID.randomUUID().toString());

      if (solarSystemDTO.getLatitude() != null && solarSystemDTO.getLongitude() != null) {
          SolarSystem solarSystem = new SolarSystem(solarSystemDTO.getToken(), solarSystemDTO.getName(), creationDate, solarSystemDTO.getType());
          solarSystem.setLatitude(solarSystemDTO.getLatitude());
          solarSystem.setLongitude(solarSystemDTO.getLongitude());
      }
      User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      SolarSystem solarSystem = new SolarSystem(solarSystemDTO.getToken(), solarSystemDTO.getName(), creationDate, solarSystemDTO.getType());
      solarSystem.setRelationOwnedBy(user);
      solarSystemRepository.save(solarSystem);

      user.addMySystems(solarSystem);
      userRepository.save(user);
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
