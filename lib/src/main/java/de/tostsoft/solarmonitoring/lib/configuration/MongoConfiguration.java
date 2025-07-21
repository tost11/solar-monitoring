package de.tostsoft.solarmonitoring.lib.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
@EnableMongoRepositories(basePackages = {"de.tostsoft.solarmonitoring"})
public class MongoConfiguration {

  @Bean
  public ValidatingMongoEventListener validatingMongoEventListener(
          final LocalValidatorFactoryBean factory) {
    return new ValidatingMongoEventListener(factory);
  }
}
