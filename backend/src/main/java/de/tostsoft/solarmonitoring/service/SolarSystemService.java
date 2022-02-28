package de.tostsoft.solarmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.model.ManageBY;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.utils.NumberComparator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.InternalNode;
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

  @Autowired
  private Driver driver;

  private ObjectMapper neo4jObjectMapper = new ObjectMapper();

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem){
    return convertSystemToDTO(solarSystem,false);
  }

  public void init(){
    neo4jObjectMapper.registerModule(new JavaTimeModule());
  }

  public SolarSystemDTO convertSystemToDTO(SolarSystem solarSystem,boolean withManagers) {
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
        .managers(withManagers?convertToManagerDTO(solarSystem.getRelationManageBy()):null)
        .build();
  }

  private List<ManagerDTO> convertToManagerDTO(List<ManageBY> manageBy) {
    return manageBy.stream().map(manageBY -> new ManagerDTO(manageBY.getUser().getId(),manageBY.getUser().getName(),manageBY.getPermissions())).collect(Collectors.toList());
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

     // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
  }

  public SolarSystemDTO getSystemWithUserFromContext(long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnsAndRelationManageByAdminOrManageReturnEverything(id, user.getId());
    if (solarSystem == null) {
      return  null;
    }
    boolean showManagers = solarSystem.getRelationOwnedBy().getId().equals(user.getId());
    if (!showManagers) {
      showManagers = solarSystem.getRelationManageBy().stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));
    }
    return convertSystemToDTO(solarSystem,showManagers);
  }

  public List<SolarSystemListItemDTO> getSystems() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var fullUser= userRepository.findByIdAndLoadRelations(user.getId());
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
        solarSystem.addLabel(Neo4jLabels.IS_DELETED.toString());
        solarSystemRepository.save(solarSystem);
        return ResponseEntity.status(HttpStatus.OK).body("System ist Deleted");
    }

  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO,SolarSystem solarSystem) {
    var systemNode = Cypher.node(""+Neo4jLabels.SolarSystem).named("s");
    List<Expression> ops = new ArrayList<>();
    if(!StringUtils.equals(newSolarSystemDTO.getName(),solarSystem.getName())){
      ops.add(systemNode.property("name").to(Cypher.literalOf(newSolarSystemDTO.getName())));
    }
    if(!NumberComparator.compare(newSolarSystemDTO.getMaxSolarVoltage(),solarSystem.getMaxSolarVoltage())){
      ops.add(systemNode.property("maxSolarVoltage").to(Cypher.literalOf(newSolarSystemDTO.getMaxSolarVoltage())));
    }
    if(newSolarSystemDTO.getIsBatteryPercentage() != solarSystem.getIsBatteryPercentage()){
      ops.add(systemNode.property("batteryPercentage").to(Cypher.literalOf(newSolarSystemDTO.getIsBatteryPercentage())));
    }
    if(!NumberComparator.compare(newSolarSystemDTO.getBatteryVoltage(),solarSystem.getBatteryVoltage())){
      ops.add(systemNode.property("batteryVoltage").to(Cypher.literalOf(newSolarSystemDTO.getBatteryVoltage())));
    }
    if(!NumberComparator.compare(newSolarSystemDTO.getInverterVoltage(),solarSystem.getInverterVoltage())){
      ops.add(systemNode.property("inverterVoltage").to(Cypher.literalOf(newSolarSystemDTO.getInverterVoltage())));
    }
    if(!NumberComparator.compare(newSolarSystemDTO.getLongitude(),solarSystem.getLongitude())){
      ops.add(systemNode.property("longitude").to(Cypher.literalOf(newSolarSystemDTO.getLongitude())));
    }
    if(!NumberComparator.compare(newSolarSystemDTO.getLatitude(),solarSystem.getLatitude())){
      ops.add(systemNode.property("latitude").to(Cypher.literalOf(newSolarSystemDTO.getLongitude())));
    }

    if(ops.isEmpty()){
      //nothing todo here
      return  convertSystemToDTO(solarSystem);
    }

    var statement = Cypher.match(systemNode).where(systemNode.internalId().eq(Cypher.literalOf(solarSystem.getId()))).set(ops).returning(systemNode).build();

    var res = driver.session().writeTransaction(tx->tx.run(statement.getCypher()).single());
    var resultNode = (InternalNode)res.get(0).asObject();

    var resSol = neo4jObjectMapper.convertValue(resultNode.asMap(),SolarSystem.class);
    resSol.setId(resultNode.id());
    return  convertSystemToDTO(resSol);
  }

  public SolarSystemDTO addManageUser(String userName,long solarSystemDTO,Permissions permissions) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem system = solarSystemRepository.findByIdAndRelationOwnedById(solarSystemDTO,user.getId());
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
