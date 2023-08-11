package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface SolarSystemRepository extends MongoRepository<SolarSystem,String> {

  List<SolarSystem> findAllByIdIn(Collection<String> ids);

  List<SolarSystem> findAllByPublicMode(PublicMode publicMode);

  List<SolarSystem> findAllByPublicModeIsNot(PublicMode publicMode);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastManualCalculation' : ?1 } }")
  void updateLastManualCalculation(String id, long time);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastCalculation' : ?1 } }")
  void updateLastCalculation(String id, long time);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'token' : ?1 } }")
  void updateToken(String id, String token);

  List<SolarSystem> findAllByLastCalculationIsNull();

  List<SolarSystem> findAllByLastCalculationIsLessThan(long dateTime);

  Optional<SolarSystem> findByIdAndOwnedById(String id, String ownedBy);

  Optional<SolarSystem> findByInfluxTagName(String name);

  List<SolarSystem> findByTypeAndOwnedById(SolarSystemType type,String id);

  @Query("{ 'ownedBy._id':?1 , 'type': ?0}")
  List<SolarSystem> seesAllFindByTypeAndOwnedById(SolarSystemType type,String id);

  @Query("{ 'ownedBy._id': ?0 , 'deletedAt': {$exists: false}}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllActiveSystemsByOwner(String id, LocalDateTime dateTime);

  Optional<SolarSystem> findByIdAndPublicModeIsNot(String id,PublicMode publicMode);
}
