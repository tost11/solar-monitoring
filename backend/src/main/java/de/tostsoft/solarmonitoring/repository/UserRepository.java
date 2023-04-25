package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.configuration.SeesSoftlyDeletedRecords;
import de.tostsoft.solarmonitoring.configuration.SoftDeleteMongoRepositoryFactoryBean;
import de.tostsoft.solarmonitoring.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

public interface UserRepository extends MongoRepository<User,String> {

  long countByName(String name);

  User findByName(String name);

  long countByIdAndIsAdmin(String name,boolean admin);

  List<User> findAllByNameStartingWith(String start);

  @Query("{ 'name':{'$regex':'^?0'}}")
  List<User> seesAllFindAllByNameStartingWith(String start);

  @Query("{ '_id': ?0}")
  Optional<User> seesAllFindById(String id);
}
