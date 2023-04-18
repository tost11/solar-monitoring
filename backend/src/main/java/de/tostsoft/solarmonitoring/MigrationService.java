package de.tostsoft.solarmonitoring;


import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.ViewData;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MigrationService {

  @Autowired
  private Neo4jUserRepository neo4jUserRepository;

  @Autowired
  private Neo4jSolarSystemRepository neo4jSolarSystemRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserRepository deletedUserRepository;

  @Autowired
  private SolarSystemRepository solarSystemRepository;

  @Autowired
  private ManagesRepository managesRepository;

  @Autowired
  private InfluxConnection influxConnection;

  //@PostConstruct
  public void migrate(){

    neo4jUserRepository.initNameConstrain();

    solarSystemRepository.deleteAll();
    userRepository.deleteAll();
    managesRepository.deleteAll();

    //migrate users#
    var neo4jUsers = neo4jUserRepository.findAll();

    var userMap = new HashMap<Long,User>();

    for (var neo4jUser : neo4jUsers) {

      var user = User.builder()
          .numAllowedSystems(neo4jUser.getNumAllowedSystems())
          .viewName(StringUtils.lowerCase(neo4jUser.getName()))
          .name(neo4jUser.getName())
          .creationDate(neo4jUser.getCreationDate())
          .isAdmin(neo4jUser.getIsAdmin())
          .password(neo4jUser.getPassword())
          .deletedAt(neo4jUser.getLabels().contains(Neo4jLabels.IS_DELETED.toString())? ZonedDateTime.now():null)
          .influxBucketName("user-"+neo4jUser.getId())
          .manges(new ArrayList<>())
          .owns(new ArrayList<>())
          .build();

      user = userRepository.save(user);


      userMap.put(neo4jUser.getId(),user);
    }

    var neo4jSolarSystems = neo4jSolarSystemRepository.findAll();

    var systemMap = new HashMap<Long,SolarSystem>();

    for (var neo4jSolarSystem : neo4jSolarSystems) {

      var vd = ViewData.builder()
          .isBatteryPercentage(neo4jSolarSystem.getIsBatteryPercentage())
          .hasACInput(neo4jSolarSystem.getHasACInput())
          .hasDCOutput(neo4jSolarSystem.getHasDCOutput())
          .hasACOutput(neo4jSolarSystem.getHasACOutput())
          .showAmpere(neo4jSolarSystem.getShowAmpere())
          .voltageAC(neo4jSolarSystem.getVoltageAC())
          .batteryVoltage(neo4jSolarSystem.getBatteryVoltage())
          .maxSolarVoltage(neo4jSolarSystem.getMaxSolarVoltage())
          .build();

      var solarSystem = SolarSystem.builder()
         .name(StringUtils.lowerCase(neo4jSolarSystem.getName()))
         .viewName(neo4jSolarSystem.getName())
         .token(neo4jSolarSystem.getToken())
         .creationDate(neo4jSolarSystem.getCreationDate())
         .buildingDate(neo4jSolarSystem.getBuildingDate())
         .type(neo4jSolarSystem.getType())
         .deletedAt(neo4jSolarSystem.getLabels().contains(Neo4jLabels.IS_DELETED.toString())? ZonedDateTime.now():null)
         .latitude(neo4jSolarSystem.getLatitude())
         .longitude(neo4jSolarSystem.getLongitude())
         .viewData(vd)
         .publicMode(neo4jSolarSystem.getPublicMode())
         .timezone(neo4jSolarSystem.getTimezone())
         .lastCalculation(neo4jSolarSystem.getLastCalculation())
         .lastManualCalculation(neo4jSolarSystem.getLastManualCalculation())
         .influxTagName(""+neo4jSolarSystem.getId())
         //.ownedBy(userMap.get(neo4jSolarSystem.getRelationOwnedBy().getId()))
         .build();

      solarSystem.setOwnedBy(userMap.get(neo4jSolarSystem.getRelationOwnedBy().getId()));

      solarSystem = solarSystemRepository.save(solarSystem);

      systemMap.put(neo4jSolarSystem.getId(),solarSystem);
    }

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
  }

}
