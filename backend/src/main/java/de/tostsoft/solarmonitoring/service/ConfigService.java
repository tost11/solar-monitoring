package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.Config;
import de.tostsoft.solarmonitoring.repository.ConfigRepository;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);

  @Autowired
  private ConfigRepository configRepository;

  @Value("${configNode:root}")
  private String configName;

  @PostConstruct
  private void init(){
    configRepository.initNameConstrain();

    var config = configRepository.findByName(configName);
    if(config  == null){
      LOG.info("Config node is missing it will be created, name {}",configName);
      Config c = Config.builder().name(configName).isRegistrationEnabled(true).build();
      c = configRepository.save(c);
      LOG.info(c.toString());
    }else{
      LOG.info("Loaded config, name {}",configName);
      LOG.info(config.toString());
    }
  }

  public boolean isRegistrationEnabled(){
    return configRepository.findByName(configName).getIsRegistrationEnabled();
  }

  public void setRegistrationEnabled(boolean enabled){
    configRepository.setRegistrationEnabled(configName,enabled);
  }

}
