package de.tostsoft.solarmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.NewTokenDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemListItemDTO;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

  @PostConstruct
  public void init(){
    neo4jObjectMapper.registerModule(new JavaTimeModule());
  }

  private SolarSystem updateWithoutRelations(SolarSystem oldSystem,SolarSystemDTO newSystem){

    List<Pair<String,Object>> properties = new ArrayList<>();
    if(!StringUtils.equals(newSystem.getName(),oldSystem.getName())){
      properties.add(new ImmutablePair<>("name",Cypher.literalOf(newSystem.getName())));
    }
    if(!NumberComparator.compare(newSystem.getMaxSolarVoltage(),oldSystem.getMaxSolarVoltage())){
      properties.add(new ImmutablePair<>("maxSolarVoltage",newSystem.getMaxSolarVoltage()));
    }
    if(newSystem.getIsBatteryPercentage() != oldSystem.getIsBatteryPercentage()){
      properties.add(new ImmutablePair<>("batteryPercentage",newSystem.getIsBatteryPercentage()));
    }
    if(!NumberComparator.compare(newSystem.getBatteryVoltage(),oldSystem.getBatteryVoltage())){
      properties.add(new ImmutablePair<>("batteryVoltage",newSystem.getBatteryVoltage()));
    }
    if(!NumberComparator.compare(newSystem.getInverterVoltage(),oldSystem.getInverterVoltage())){
      properties.add(new ImmutablePair<>("inverterVoltage",newSystem.getInverterVoltage()));
    }
    if(!NumberComparator.compare(newSystem.getLongitude(),oldSystem.getLongitude())){
      properties.add(new ImmutablePair<>("longitude",newSystem.getLongitude()));
    }
    if(!NumberComparator.compare(newSystem.getLatitude(),oldSystem.getLatitude())){
      properties.add(new ImmutablePair<>("latitude",newSystem.getLatitude()));
    }

    return updateWithoutRelations(oldSystem,properties,new ArrayList<>(),new ArrayList<>());
  }

  private SolarSystem updateWithoutRelations(SolarSystem oldSystem,List<Pair<String,Object>> propertiesToChange) {
    return updateWithoutRelations(oldSystem,propertiesToChange,new ArrayList<>(),new ArrayList<>());
  }

  private SolarSystem updateWithoutRelations(SolarSystem oldSystem,List<Pair<String,Object>> propertiesToChange,List<String> labelsToRemove, List<String> labelsToAdd){

    if(propertiesToChange.isEmpty() && labelsToRemove.isEmpty() && labelsToAdd.isEmpty()){
      //nothing todo here
      return oldSystem;
    }

    final String nodeName = "s";

    var systemNode = Cypher.node(""+Neo4jLabels.SolarSystem).named(nodeName);
    List<Expression> ops = propertiesToChange.stream().map(v->systemNode.property(v.getLeft()).to(Cypher.literalOf(v.getRight()))).collect(Collectors.toList());
    var statement = Cypher.match(systemNode).where(systemNode.internalId().eq(Cypher.literalOf(oldSystem.getId()))).set(ops).build();
    var queryString = statement.getCypher();

    queryString += " ";

    if(!labelsToAdd.isEmpty()) {
      for(int i =0;i<labelsToAdd.size();i++){
        if(i==0){
          queryString+=nodeName;
        }
        queryString += ":"+Cypher.literalOf(labelsToAdd.get(i))+ "";
      }
      queryString+=" ";
    }

    if(!labelsToRemove.isEmpty()) {
      queryString += "REMOVE ";
      for(int i =0;i<labelsToRemove.size();i++){
        if(i==0){
          queryString+=nodeName;
        }
        queryString += ":"+Cypher.literalOf(labelsToRemove.get(i))+ "";
      }
      queryString+=" ";
    }

    queryString += "RETURN "+nodeName;

    final String q = queryString;

    var res = driver.session().writeTransaction(tx->tx.run(q).single());
    var resultNode = (InternalNode)res.get(0).asObject();
    var resSol = neo4jObjectMapper.convertValue(resultNode.asMap(), SolarSystem.class);
    resSol.setId(resultNode.id());
    return resSol;
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
    return manageBy.stream().map(manageBY -> new ManagerDTO(manageBY.getUser().getId(),manageBY.getUser().getName(),manageBY.getPermission())).collect(Collectors.toList());
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
    if(userRepository.countByRelationOwns(user.getId()) >= user.getNumAllowedSystems()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You have to much Systems");
    }

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
  }

  public SolarSystemDTO getSystemWithUserFromContext(long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMangeWithRelations(id, user.getId());
    if (solarSystem == null) {
      return  null;
    }
    //check if user is permitted to se see and mange editors
    boolean showManagers = solarSystem.getRelationOwnedBy().getId().equals(user.getId()) ||
        solarSystem.getRelationManageBy().stream().anyMatch(u->u.getUser().getId().longValue() == user.getId().longValue() && u.getPermission() == Permissions.ADMIN);
    return convertSystemToDTO(solarSystem,showManagers);
  }

  public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var fullUser= userRepository.findById(user.getId()).get();
    ArrayList<SolarSystemListItemDTO> collect = new ArrayList<>();
      for (SolarSystem system : fullUser.getRelationOwns()) {
          collect.add(convertSystemToListItemDTO(system, "owns"));
    }
    for (Manages system : fullUser.getRelationManageBy()) {
      collect.add(convertSystemToListItemDTO(system.getSolarSystem(), system.getPermission().toString()));
    }

    return collect;
  }

  public ResponseEntity<String> deleteSystem(SolarSystem solarSystem)  {
        solarSystemRepository.addLabel(solarSystem.getId(),Neo4jLabels.IS_DELETED.toString());
        return ResponseEntity.status(HttpStatus.OK).body("System ist Deleted");
    }

  public SolarSystemDTO patchSolarSystem(SolarSystemDTO newSolarSystemDTO,SolarSystem solarSystem) {
    var res = updateWithoutRelations(solarSystem,newSolarSystemDTO);
    return  convertSystemToDTO(res);
  }


  public NewTokenDTO createNewToken(SolarSystem solarSystem) {
    String token = UUID.randomUUID().toString();
    updateWithoutRelations(solarSystem, Collections.singletonList(new ImmutablePair<>("token",passwordEncoder.encode(token))));
    return new NewTokenDTO(token);
  }

  public SolarSystem findSystemWithFullAccess(long systemId,boolean loadRelations) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if(loadRelations) {
      return solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(systemId, user.getId());
    }
    return solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdmin(systemId,user.getId());
  }
}
