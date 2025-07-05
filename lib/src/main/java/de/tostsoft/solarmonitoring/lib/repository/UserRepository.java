package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.configuration.SeesSoftlyDeletedRecords;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {

  long countByName(String name);

  long countByMail(String mail);

  User findByNameOrMail(String name,String mail);

  User findByName(String name);

  long countByIdAndIsAdmin(String name,boolean admin);

  List<User> findAllByNameStartingWith(String start);

  @Query("{ 'name':{'$regex':'^?0'}}")
  List<User> seesAllFindAllByNameStartingWith(String start);

  @Query("{ '_id': ?0}")
  Optional<User> seesAllFindById(String id);

  @SeesSoftlyDeletedRecords
  Optional<User> findByInfluxBucketName(String bucketName);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'mail' : ?1 } }")
  void updateMailByUserId(String id, String mail);
}
