package de.tostsoft.solarmonitoring;


import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

@Service
public class MigrationService {

  @Autowired
  private Neo4jUserRepository neo4jUserRepository;
  @Autowired
  private Neo4jSolarSystemRepository neo4jSolarSystemRepository;

  @Autowired
  UserRepository userRepository;

  @Autowired
  SolarSystemRepository solarSystemRepository;

  @Autowired
  ManagesRepository managesRepository;

  @PostConstruct
  public void migrate(){

    solarSystemRepository.deleteAll();
    userRepository.deleteAll();

    //migrate users#
    var neo4jUsers = neo4jUserRepository.findAll();

    var userMap = new HashMap<Long,User>();

    for (var neo4jUser : neo4jUsers) {

      var user = User.builder()
          .numAllowedSystems(neo4jUser.getNumAllowedSystems())
          .name(neo4jUser.getName())
          .creationDate(neo4jUser.getCreationDate())
          .isAdmin(neo4jUser.getIsAdmin())
          .isDeleted(neo4jUser.getLabels().contains(Neo4jLabels.IS_DELETED.toString()))
          .password(neo4jUser.getPassword())
          .manges(new ArrayList<>())
          .owns(new ArrayList<>())
          .build();

      user = userRepository.save(user);

      userMap.put(neo4jUser.getId(),user);
    }

    var neo4jSolarSystems = neo4jSolarSystemRepository.findAll();

    var systemMap = new HashMap<Long,SolarSystem>();

    for (var neo4jSolarSystem : neo4jSolarSystems) {

       var solarSystem = SolarSystem.builder()
           .name(neo4jSolarSystem.getName())
           .token(neo4jSolarSystem.getToken())
           .creationDate(neo4jSolarSystem.getCreationDate())
           .buildingDate(neo4jSolarSystem.getBuildingDate())
           .type(neo4jSolarSystem.getType())
           .isDeleted(neo4jSolarSystem.getLabels().contains(Neo4jLabels.IS_DELETED.toString()))
           .latitude(neo4jSolarSystem.getLatitude())
           .longitude(neo4jSolarSystem.getLongitude())
           .isBatteryPercentage(neo4jSolarSystem.getIsBatteryPercentage())
           .hasACInput(neo4jSolarSystem.getHasACInput())
           .hasDCOutput(neo4jSolarSystem.getHasDCOutput())
           .hasACOutput(neo4jSolarSystem.getHasACOutput())
           .showAmpere(neo4jSolarSystem.getShowAmpere())
           .voltageAC(neo4jSolarSystem.getVoltageAC())
           .batteryVoltage(neo4jSolarSystem.getBatteryVoltage())
           .maxSolarVoltage(neo4jSolarSystem.getMaxSolarVoltage())
           .publicMode(neo4jSolarSystem.getPublicMode())
           .timezone(neo4jSolarSystem.getTimezone())
           .lastCalculation(neo4jSolarSystem.getLastCalculation())
           .lastManualCalculation(neo4jSolarSystem.getLastManualCalculation())
           //.ownedBy(userMap.get(neo4jSolarSystem.getRelationOwnedBy().getId()))
           .build();

      solarSystem = solarSystemRepository.save(solarSystem);

      systemMap.put(neo4jSolarSystem.getId(),solarSystem);
    }

    for (Neo4jSolarSystem neo4jSolarSystem : neo4jSolarSystems) {
      userMap.get(neo4jSolarSystem.getRelationOwnedBy().getId()).getOwns().add(systemMap.get(neo4jSolarSystem.getId()));
    }

    userRepository.saveAll(userMap.values());

    for (Neo4jSolarSystem neo4jSolarSystem : neo4jSolarSystems) {
      for (Neo4jManageBy neo4jManageBy : neo4jSolarSystem.getRelationNeo4jManageBy()) {
        var manges = Manages.builder()
            .user(userMap.get(neo4jManageBy.getNeo4jUser().getId()))
            .permission(neo4jManageBy.getPermission())
            .solarSystem(systemMap.get(neo4jManageBy.getId()))
            .build();

        managesRepository.save(manges);
      }
    }

    var test = solarSystemRepository.findAll();

    System.out.println("test");
  }

}
