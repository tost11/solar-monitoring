package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.Config;
import de.tostsoft.solarmonitoring.lib.repository.ConfigRepository;
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
  private ConfigRepository configRepository;

  @Value("${configNode:root}")
  private String configName;

  @Value("${maxDailyRegistrations:0}")
  private int maxDailyRegistrations;

  @PostConstruct
  private void init(){
    var config = configRepository.findByName(configName);
    if(config == null){
      LOG.info("Config node is missing it will be created, name {}",configName);
      var c = Config.builder()
              .name(configName)
              .isRegistrationEnabled(true)
              .dailyRegistrations(10)
              .build();
      c = configRepository.save(c);
      LOG.info(c.toString());
    }else{
      LOG.info("Loaded config, name {}",configName);
      LOG.info(config.toString());
    }
  }

  public boolean isRegistrationEnabled(){
    var config =  configRepository.findByName(configName);
    return config != null && config.getIsRegistrationEnabled();
  }

  public void setRegistrationEnabled(boolean enabled){
    configRepository.setRegistrationEnabled(configName,enabled);
  }

  public boolean limitRegistrationReached(){
    if(maxDailyRegistrations <= 0){
      return false;
    }
    return configRepository.findByName(configName).getDailyRegistrations() >= maxDailyRegistrations;
  }

  public void increaseDailyRegistrations(){
    configRepository.increaseDailyRegistrations(configName,1);
  }
}
