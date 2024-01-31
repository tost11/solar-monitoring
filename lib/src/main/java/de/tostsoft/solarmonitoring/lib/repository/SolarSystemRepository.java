package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.TotalValues;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SolarSystemRepository extends MongoRepository<SolarSystem,String> {

  @Query("{$or:[{ '_id' : {$in : ?0 } },{ 'shortener' : {$in : ?0 } }]}")
  List<SolarSystem> findAllByIdOrShortenerIn(Collection<String> ids);

  @Query("{$or:[{ '_id' : ?0 },{ 'shortener' : ?0 }]}")
  List<SolarSystem> findAllByIdOrShortener(String id);

  List<SolarSystem> findAllByNeedsStatisticRecalculation(boolean needs);

  boolean existsByShortener(String shortener);

  boolean existsByShortenerAndIdNot(String shortener,String id);

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

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'totalValues' : ?1 , needsStatisticRecalculation: false} }")
  void updateTotalValues(String id, TotalValues totalValues);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'needsStatisticRecalculation' : ?1 } }")
  void updateNeedsStatisticRecalculation(String id, boolean totalValues);

  @Query("{$and : [{ '_id' : ?0}, { $or : [{ 'currentValues.lastSet': { $exists: false }},{ 'currentValues.lastSet' : { $lt : ?1 }}] }]}")
  @Update("{ '$set' : { 'currentValues' : ?2 } }")
  void updateCurrentValuesIfNewer(String id, Long lastSet, CurrentValues currentValues);

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
