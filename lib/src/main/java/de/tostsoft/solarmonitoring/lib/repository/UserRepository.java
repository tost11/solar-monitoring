package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {

  @NotNull
  @Query(value = "{$and:[{'_id':?0},{'deletedAt': null}]}")
  @Override
  Optional<User> findById(@NotNull String id);

  @Query(value = "{'_id':?0}")
  Optional<User> findByIdWithDeleted(String id);

  @NotNull
  @Query(value = "{'deletedAt': null}")
  @Override
  List<User> findAll();

  @Query(value = "{}")
  Optional<User> findAllWithDeleted(String id);

  @Query(value = "{ 'name': ?0 }",count = true)
  long countByNameWithDeleted(String name);

  @Query(value = "{ 'mail': ?0 }",count = true)
  long countByMailWithDeleted(String mail);

  @Query(value = "{'$and':[{'$or':[{ 'name': ?0},{ 'mail': ?1}]},{'deletedAt': null}]}")
  User findOneByNameOrMail(String name,String mail);

  @Query(value = "{'$and':[{ 'name': ?0},{'deletedAt': null}]}")
  User findByName(String name);

  @Query(value = "{'$and':[{ 'id': ?0},{'isAdmin': ?1}]}",count = true)
  long countByIdAndIsAdminWithDeleted(String name, boolean admin);

  @Query(value = "{'$and':[{'name':{'$regex':'^?0'}},{'deletedAt': null}]}")
  List<User> findAllByNameStartingWith(String start);

  @Query("{ 'name':{'$regex':'^?0'}}")
  List<User> findAllByNameStartingWithWithDeleted(String start);

  @Query(value = "{ 'influxBucketName': ?0 }")
  Optional<User> findByInfluxBucketNameWithDeleted(String bucketName);

  @Query("{}")
  List<User> findAllWithDeleted();

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'mail' : ?1 } }")
  void updateMailByUserId(String id, String mail);

  Page<User> findAllByDeletedAtBefore(LocalDateTime time, Pageable pageable);

  @Query("{ '_id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAt(String id, LocalDateTime dateTime);
}
