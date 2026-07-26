package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Validated
public interface SolarSystemRepository extends SoftDeleteMongoRepository<SolarSystem,String> {

  @Query("{$and:[{$or:[{ '_id' : {$in : ?0 } },{ 'shortener' : {$in : ?0 } }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByIdOrShortenerIn(@NotNull Collection<String> ids);

  @Query("{$and:[{$or:[{ '_id' : ?0 },{ 'shortener' : ?0 }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByIdOrShortener(@NotNull String id);

  @Query(value = "{$and:[{'deyeSunSerials':{$elemMatch:{$eq:?0}}},{'deletedAt': null}]}")
  Optional<SolarSystem> findSolarSystemBySerialInAndDeyeSunSerials(@NotNull Long serial);

  @Query(value = "{$and:[{'needsStatisticRecalculation':?0},{'deletedAt': null}]}")
  List<SolarSystem> findAllByNeedsStatisticRecalculation(boolean needs);

  @Query(value = "{$and:[{'shortener':?0},{'deletedAt': null}]}",exists = true)
  boolean existsByShortener(@NotNull String shortener);

  @Query(value = "{$and:[{shortener:?0},{'_id': {'$ne':?1}}]}",exists = true)
  boolean existsByShortenerAndIdNotWithDeleted(@NotNull String shortener,@NotNull String id);

  @Query(value = "{$and:[{'publicMode':?0},{'deletedAt': null}]}")
  List<SolarSystem> findAllByPublicMode(@NotNull PublicMode publicMode);

  @Query(value = "{$and:[{'publicMode':{'$ne':?0}},{'deletedAt': null}]}")
  List<SolarSystem> findAllByPublicModeIsNot(@NotNull PublicMode publicMode);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastManualCalculation' : ?1 } }")
  void updateLastManualCalculation(@NotNull String id, long time);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastCalculation' : ?1 } }")
  void updateLastCalculation(@NotNull String id, long time);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'totalValues' : ?1 , needsStatisticRecalculation: false} }")
  void updateTotalValues(@NotNull String id,@NotNull  TotalValues totalValues);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'needsStatisticRecalculation' : ?1 } }")
  void updateNeedsStatisticRecalculation(@NotNull String id, boolean needsReaclulation);

  @Query("{$and : [{ '_id' : ?0}, { $or : [{ 'currentValues.lastSet': { $exists: false }},{ 'currentValues.lastSet' : { $lt : ?1 }}] }]}")
  @Update("{ '$set' : { 'currentValues' : ?2 } }")
  void updateCurrentValuesIfNewer(@NotNull String id,@NotNull  Long lastSet,@NotNull CurrentValues currentValues);

  @Query(value = "{$and:[{'lastCalculation':null},{'deletedAt': null}]}")
  List<SolarSystem> findAllByLastCalculationIsNull();

  @Query(value = "{$and:[{'lastCalculation':{$lt: ?0}},{'deletedAt': null}]}")
  List<SolarSystem> findAllByLastCalculationIsLessThan(long dateTime);

  @Query(value = "{$and:[{$and:[{'_id':?0},{'ownedBy._id':?1}]},{'deletedAt': null}]}")
  Optional<SolarSystem> findByIdAndOwnedById(@NotNull String id,@NotNull String ownedBy);

  @Query(value = "{$and:[{'influxTagName':?0},{'deletedAt': null}]}")
  Optional<SolarSystem> findByInfluxTagName(@NotNull String name);

  @Query(value = "{$and:[{$and:[{'type':?0},{'ownedBy._id':?1}]},{'deletedAt': null}]}")
  List<SolarSystem> findByTypeAndOwnedById(@NotNull SolarSystemType type,@NotNull String id);

  @Query("{ 'ownedBy._id':?1 , 'type': ?0}")
  List<SolarSystem> findByTypeAndOwnedByIdWithDeleted(@NotNull SolarSystemType type,@NotNull String id);

  @Query("{ 'ownedBy._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllActiveSystemsByOwner(@NotNull String id,@NotNull LocalDateTime dateTime);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'lastOnlineCheckStatus' : ?1 } }")
  void updateLastOnlineCheckStatus(@NotNull String id, boolean totalValues);

  @Query("{$and:[{ 'tags' :  {$in: [{ $oid :?0}]}},{'deletedAt': null}]}")
  List<SolarSystem> findAllByTagsContains(@NotNull String tagId);

  @Query("{$and:[{$and:[{ 'tags' : {$in: [{ $oid :?0}]} },{ 'publicMode' : {$ne : ?1 } }]},{'deletedAt': null}]}")
  List<SolarSystem> findAllByTagsContainsAndPublicModeIsNot(@NotNull String tagId,@NotNull PublicMode publicMode);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$push' : { 'tokens' : ?1 } }")
  void addToken(@NotNull String id, @NotNull AccessToken token);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$pull' : { 'tokens' : { 'id' : ?1 } } }")
  void removeToken(@NotNull String id, @NotNull String tokenId);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'tokens' : ?1 } }")
  void updateTokens(@NotNull String id, @NotNull List<AccessToken> tokens);
}
