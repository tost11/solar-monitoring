package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.Neo4jConfig;
import de.tostsoft.solarmonitoring.repository.Neo4jConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);

  @Autowired
  private Neo4jConfigRepository neo4jConfigRepository;

  @Value("${configNode:root}")
  private String configName;

  @PostConstruct
  private void init(){
    neo4jConfigRepository.initNameConstrain();

    var config = neo4jConfigRepository.findByName(configName);
    if(config  == null){
      LOG.info("Config node is missing it will be created, name {}",configName);
      Neo4jConfig c = Neo4jConfig.builder().name(configName).isRegistrationEnabled(true).build();
      c = neo4jConfigRepository.save(c);
      LOG.info(c.toString());
    }else{
      LOG.info("Loaded config, name {}",configName);
      LOG.info(config.toString());
    }
  }

  public boolean isRegistrationEnabled(){
    return neo4jConfigRepository.findByName(configName).getIsRegistrationEnabled();
  }

  public void setRegistrationEnabled(boolean enabled){
    neo4jConfigRepository.setRegistrationEnabled(configName,enabled);
  }

}
