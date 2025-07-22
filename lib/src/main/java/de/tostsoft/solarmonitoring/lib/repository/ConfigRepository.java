package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Config;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Validated
public interface ConfigRepository extends MongoRepository<Config,String> {

  Config findByName(@NotNull String name);

  @Query("{ 'name' : ?0 }")
  @Update("{ '$set' : { 'isRegistrationEnabled' : ?1 } }")
  void setRegistrationEnabled(@NotNull String name,@NotNull Boolean value);

  @Query("{ 'name' : ?0 }")
  @Update("{ $inc: { dailyRegistrations: ?1 } }")
  void increaseDailyRegistrations(@NotNull String name,int value);

  @Query("{ 'name' : ?0 }")
  @Update("{ dailyRegistrations: 0 }")
  void resetDailyRegistrations(@NotNull String name);
}
