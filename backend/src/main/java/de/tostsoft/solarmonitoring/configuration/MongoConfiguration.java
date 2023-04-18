package de.tostsoft.solarmonitoring.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"de.tostsoft.solarmonitoring.repository"}, repositoryFactoryBeanClass = SoftDeleteMongoRepositoryFactoryBean.class)
public class MongoConfiguration {
  @Bean(name = "mongoTemplate")
  CustomMongoTemplate customMongoTemplate(MongoDatabaseFactory databaseFactory, MappingMongoConverter converter) {
    return new CustomMongoTemplate(databaseFactory, converter);
  }
}
