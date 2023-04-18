package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.configuration.SeesSoftlyDeletedRecords;
import de.tostsoft.solarmonitoring.configuration.SoftDeleteMongoRepositoryFactoryBean;
import de.tostsoft.solarmonitoring.model.User;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories(repositoryFactoryBeanClass = SoftDeleteMongoRepositoryFactoryBean.class)
public interface SeesAllUserRepository extends MongoRepository<User,String> {

  @SeesSoftlyDeletedRecords
  List<User> findAllByNameStartingWith(String start);
}
