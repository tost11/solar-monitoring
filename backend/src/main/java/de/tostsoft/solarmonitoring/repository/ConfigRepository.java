package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.Config;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface ConfigRepository extends MongoRepository<Config,String> {

  Optional<Config> findByName(String name);

  @Query("{ 'name' : ?0 }")
  @Update("{ '$set' : { 'isRegistrationEnabled' : ?1 } }")
  void setRegistrationEnabled(String name,Boolean value);

}
