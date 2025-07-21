package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SolarSystemRepository extends MongoRepository<SolarSystem,String> {

  @NotNull
  @Query(value = "{$and:[{'_id':?0},{'deletedAt': null}]}")
  @Override
  Optional<SolarSystem> findById(@NotNull String id);

  @Query(value = "{'_id':?0}")
  Optional<SolarSystem> findByIdWithDeleted(String id);

  @NotNull
  @Query(value = "{'deletedAt': null}")
  @Override
  List<SolarSystem> findAll();

  @Query(value = "{}")
  List<SolarSystem> findAllWithDeleted();

  @Query("{$and:[{$or:[{ '_id' : {$in : ?0 } },{ 'shortener' : {$in : ?0 } }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByIdOrShortenerIn(Collection<String> ids);

  @Query("{$and:[{$or:[{ '_id' : ?0 },{ 'shortener' : ?0 }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByIdOrShortener(String id);

  @Query(value = "{$and:[{'deyeSunSerials':{$elemMatch:{$eq:?0}}},{'deletedAt': null}]}")
  Optional<SolarSystem> findSolarSystemBySerialInAndDeyeSunSerials(Long serial);

  @Query(value = "{$and:[{'needsStatisticRecalculation':?0},{'deletedAt': null}]}")
  List<SolarSystem> findAllByNeedsStatisticRecalculation(boolean needs);

  @Query(value = "{$and:[{'shortener':?0},{'deletedAt': null}]}",exists = true)
  boolean existsByShortener(String shortener);

  @Query(value = "{$and:[{shortener:?0},{'_id': {'$ne':?1}}]}",exists = true)
  boolean existsByShortenerAndIdNotWithDeleted(String shortener, String id);

  @Query(value = "{$and:[{'publicMode':?0},{'deletedAt': null}]}")
  List<SolarSystem> findAllByPublicMode(PublicMode publicMode);

  @Query(value = "{$and:[{'publicMode':{'$ne':?0}},{'deletedAt': null}]}")
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

  @Query(value = "{$and:[{'lastCalculation':null},{'deletedAt': null}]}")
  List<SolarSystem> findAllByLastCalculationIsNull();

  @Query(value = "{$and:[{'lastCalculation':{$lt: ?0}},{'deletedAt': null}]}")
  List<SolarSystem> findAllByLastCalculationIsLessThan(long dateTime);

  @Query(value = "{$and:[{$and:[{'_id':?0},{'ownedBy._id':?1}]},{'deletedAt': null}]}")
  Optional<SolarSystem> findByIdAndOwnedById(String id, String ownedBy);

  @Query(value = "{$and:[{'influxTagName':?0},{'deletedAt': null}]}")
  Optional<SolarSystem> findByInfluxTagName(String name);

  @Query(value = "{$and:[{$and:[{'type':?0},{'ownedBy._id':?1}]},{'deletedAt': null}]}")
  List<SolarSystem> findByTypeAndOwnedById(SolarSystemType type,String id);

  @Query("{ 'ownedBy._id':?1 , 'type': ?0}")
  List<SolarSystem> findByTypeAndOwnedByIdWithDeleted(SolarSystemType type, String id);

  @Query("{ 'ownedBy._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllActiveSystemsByOwner(String id, LocalDateTime dateTime);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastOnlineCheckStatus' : ?1 } }")
  void updateLastOnlineCheckStatus(String id, boolean totalValues);

  @Query("{$and:[{ 'tags' :  {$in: [{ $oid :?0}]}},{'deletedAt': null}]}")
  List<SolarSystem> findAllByTagsContains(String tagId);

  @Query("{$and:[{$and:[{ 'tags' : {$in: [{ $oid :?0}]} },{ 'publicMode' : {$ne : ?0 } }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByTagsContainsAndPublicModeIsNot(String tagId,PublicMode publicMode);
}
