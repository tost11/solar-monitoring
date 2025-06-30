package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Config;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.Optional;

public interface ConfigRepository extends MongoRepository<Config,String> {

  Optional<Config> findByName(String name);

  @Query("{ 'name' : ?0 }")
  @Update("{ '$set' : { 'isRegistrationEnabled' : ?1 } }")
  void setRegistrationEnabled(String name,Boolean value);

  @Query("{ 'name' : ?0 }")
  @Update("{ $inc: { dailyRegistrations: ?1 } }")
  void increaseDailyRegistrations(String name,int value);

  @Query("{ 'name' : ?0 }")
  @Update("{ dailyRegistrations: 0 }")
  void resetDailyRegistrations(String name);
}
