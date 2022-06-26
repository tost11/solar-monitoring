package de.tostsoft.solarmonitoring.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyAwesomeSolarSystemSaveRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MyAwesomeSolarSystemSaveRepository.class);

  @Autowired
  private Driver driver;

  private Map<String,Boolean> createProperties = new HashMap<>();
  private Set<String> updateProperties = new HashSet<>();

  private ObjectMapper neo4jObjectMapper = new ObjectMapper();

  @PostConstruct
  private void init(){

    neo4jObjectMapper.registerModule(new JavaTimeModule());

    createProperties.put("name",true);
    createProperties.put("maxSolarVoltage",true);
    createProperties.put("isBatteryPercentage",false);
    createProperties.put("batteryVoltage",false);
    createProperties.put("inverterVoltage",false);
    createProperties.put("longitude",false);
    createProperties.put("latitude",false);
    createProperties.put("creationDate",true);
    createProperties.put("buildingDate",false);
    createProperties.put("type",true);
    createProperties.put("token",true);
    createProperties.put("timezone",true);
    createProperties.put("publicMode",true);

    updateProperties.add("name");
    updateProperties.add("maxSolarVoltage");
    updateProperties.add("isBatteryPercentage");
    updateProperties.add("batteryVoltage");
    updateProperties.add("inverterVoltage");
    updateProperties.add("longitude");
    updateProperties.add("latitude");
    updateProperties.add("buildingDate");
    updateProperties.add("type");
    updateProperties.add("token");
    updateProperties.add("timezone");
    updateProperties.add("publicMode");
  }

  Object getProp(String methodName,SolarSystem system)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method func = SolarSystem.class.getMethod(methodName);
    return func.invoke(system);
  }

  Object parseForNeo4j(Object object){
    if(object instanceof SolarSystemType){
      return ""+object;
    }
    if(object instanceof PublicMode){
      return ""+object;
    }
    return Cypher.literalOf(object);
  }

  SolarSystem parseResult(Record record,SolarSystem oldSolarSystem){
    var resultNode = (InternalNode)record.get(0).asObject();
    var resSol = neo4jObjectMapper.convertValue(resultNode.asMap(), SolarSystem.class);
    resSol.setId(resultNode.id());
    resSol.setLabels(new HashSet<>(resultNode.labels()));
    resSol.setRelationOwnedBy(oldSolarSystem.getRelationOwnedBy());
    resSol.setRelationManageBy(oldSolarSystem.getRelationManageBy());
    return resSol;
  }

  //TODO find better way to do that
  private String neo4jEscapeLabel(String label){
    var node = Cypher.node(label);
    String q = Cypher.match(node).returning("X").build().getCypher();
    q = q.substring(0,q.lastIndexOf("`")+1);
    return q.substring(q.indexOf("`"));
  }

  public SolarSystem internalUpdateWithProps(SolarSystem solarSystem,Map<String,Object> propsToUpdate,Collection<String> labelsToAdd,Collection<String> labelsToRemove){
    final String nodeName = "s";

    if(propsToUpdate.isEmpty() && labelsToAdd.isEmpty() && labelsToRemove.isEmpty()){
      return solarSystem;
    }

    var systemNode = Cypher.node(""+Neo4jLabels.SolarSystem).named(nodeName);
    List<Expression> ops = propsToUpdate.entrySet().stream().map(v->systemNode.property(v.getKey()).to(Cypher.literalOf(v.getValue()))).collect(
        Collectors.toList());

    String queryString;
    if(propsToUpdate.isEmpty()) {
      queryString = Cypher.match(systemNode).where(systemNode.internalId().eq(Cypher.literalOf(solarSystem.getId()))).returning("X").build().getCypher();
      queryString = queryString.substring(0,queryString.length()-" RETURN X".length());//TODO find better way to to that
      if(!labelsToAdd.isEmpty()) {
        queryString += " SET";
      }
    }else{
      queryString = Cypher.match(systemNode).where(systemNode.internalId().eq(Cypher.literalOf(solarSystem.getId())))
          .set(ops).build().getCypher();
    }

    queryString += " ";

    if(!labelsToAdd.isEmpty()) {
      int i = 0;
      for (String s : labelsToAdd) {
        if (i == 0) {
          queryString += nodeName;
        }
        queryString += ":" + neo4jEscapeLabel(s);
        i++;
      }
      queryString+=" ";
    }

    if(!labelsToRemove.isEmpty()) {
      queryString += "REMOVE ";
      int i = 0;
      for (String s : labelsToRemove) {
        if(i==0){
          queryString += nodeName;
        }

        queryString += ":"+neo4jEscapeLabel(s);
        i++;
      }
      queryString += " ";
    }

    queryString += "RETURN "+nodeName;

    System.out.println(queryString);

    final String q = queryString;
    return parseResult(driver.session().writeTransaction(tx->tx.run(q).single()),solarSystem);
  }


  public SolarSystem updateSystemWithProp(SolarSystem solarSystem,String propName,Object object)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    return updateSystemWithProps(solarSystem, Collections.singletonMap(propName,object));
  }

  public SolarSystem updateSystemWithProps(SolarSystem solarSystem,Map<String,Object> givenProps)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    Map<String, Object> propsToUpdate = new HashMap<>();

    for (var prop : givenProps.entrySet()){
      String funcName = "get" + StringUtils.capitalize(prop.getKey());
      var oldFunc = SolarSystem.class.getMethod(funcName);

      if(prop.getValue() != null && prop.getValue().getClass() != oldFunc.getReturnType()){
        LOG.error("PropertyTypes not the same {} {}", prop.getValue().getClass() , oldFunc.getReturnType());
        throw new RuntimeException("PropertyTypes not the sam");
      }

      var oldVal = oldFunc.invoke(solarSystem);

      if (oldVal == null && prop.getValue() == null) {
        continue;
      }
      if (oldVal != null && oldVal.equals(prop.getValue())) {
        continue;
      }
      propsToUpdate.put(prop.getKey(), parseForNeo4j(prop.getValue()));
    }
    return internalUpdateWithProps(solarSystem,propsToUpdate,new ArrayList<>(),new ArrayList<>());
  }

  public SolarSystem updateSystem(SolarSystem solarSystem,Object newDataObject)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

    Map<String, Object> propsToUpdate = new HashMap<>();

    List<String> labelsToAdd = new ArrayList<>();
    List<String> labelsToRemove = new ArrayList<>();

    Set<String> foundLabels = null;

    try {
      var labelFunc = newDataObject.getClass().getMethod("getLabels");
      var labelObject = labelFunc.invoke(newDataObject);
      if(labelObject instanceof Collection<?>){
        var col = (Collection<?>) labelObject;
        foundLabels = new HashSet<>();
        for (Object o : col) {
          if(o instanceof String){
            foundLabels.add((String)o);
          }
        }
      }
    } catch (NoSuchMethodException ex) {
      LOG.warn("Could not parse label function of generic object");
    }

    if(foundLabels != null) {
      for (String l : foundLabels) {
        if (!solarSystem.getLabels().contains(l)) {
          labelsToAdd.add(l);
        }
      }

      for (String l : solarSystem.getLabels()) {
        if (!foundLabels.contains(l)) {
          labelsToRemove.add(l);
        }
      }
    }

    for (String prop : updateProperties){
      String funcName = "get" + StringUtils.capitalize(prop);

      Method newFunc;
      try {
        newFunc = newDataObject.getClass().getMethod(funcName);
      } catch (NoSuchMethodException ex) {
        LOG.warn("could not call function {} on {}  but is was not needed", funcName, newDataObject.getClass());
        continue;
      }

      var oldFunc = SolarSystem.class.getMethod(funcName);

      if (!newFunc.getReturnType().equals(oldFunc.getReturnType())) {
        LOG.error("PropertyTypes not the same {} {}", newFunc.getReturnType(), oldFunc.getReturnType());
        throw new RuntimeException("PropertyTypes not the sam");
      }

      var oldVal = oldFunc.invoke(solarSystem);
      var newVal = newFunc.invoke(newDataObject);

      if (oldVal == null && newVal == null) {
        continue;
      }
      if (oldVal != null && oldVal.equals(newVal)) {
        continue;
      }
      propsToUpdate.put(prop, parseForNeo4j(newVal));
    }

    return internalUpdateWithProps(solarSystem,propsToUpdate,labelsToAdd,labelsToRemove);
  }

  public SolarSystem createNewSystem(SolarSystem solarSystem)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    //extract properties from object
    Map<String,Object> propertiesToAdd = new HashMap<>();

    for (Entry<String, Boolean> prop : createProperties.entrySet()) {
      String funcName = "get"+ StringUtils.capitalize(prop.getKey());
      Object res = getProp(funcName,solarSystem);
      if(prop.getValue() && res == null){
        throw new RuntimeException("Value of "+prop.getKey()+" is null but is mandetory");
      }
      propertiesToAdd.put(prop.getKey(),parseForNeo4j(res));
    }

    //real query stuff
    final String systemName = "s";
    final String userName = "u";

    var labelsToAdd = solarSystem.getLabels().stream().filter(l->!l.equals(Neo4jLabels.SolarSystem.toString())).collect(Collectors.toList());

    final var userNode = Cypher.node(""+ Neo4jLabels.User).named(userName);
    final var systemNode = Cypher.node(""+Neo4jLabels.SolarSystem,labelsToAdd).named(systemName).withProperties(propertiesToAdd);

    var cypher = Cypher.match(userNode).where(userNode.internalId().eq(Cypher.literalOf(solarSystem.getRelationOwnedBy().getId()))).create(systemNode).build().getCypher();

    var queryString = cypher +" <- [r:owns] - (" + userName + ") RETURN " + systemName;

    System.out.println(queryString);

    final String q = queryString;

    return parseResult(driver.session().writeTransaction(tx->tx.run(q).single()),solarSystem);
  }

}
