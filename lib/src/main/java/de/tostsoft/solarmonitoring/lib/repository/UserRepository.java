package de.tostsoft.solarmonitoring.lib.repository;

import com.mongodb.lang.NonNull;
import de.tostsoft.solarmonitoring.lib.model.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Validated
public interface UserRepository extends SoftDeleteMongoRepository<User,String> {

  @Query(value = "{ 'name': ?0 }",count = true)
  long countByNameWithDeleted(@NotNull String name);

  @Query(value = "{ 'mail': ?0 }",count = true)
  long countByMailWithDeleted(@NotNull String mail);

  @Query(value = "{'$and':[{'$or':[{ 'name': ?0},{ 'mail': ?1}]},{'deletedAt': null}]}")
  User findOneByNameOrMail(@NotNull String name, @NotNull String mail);

  @Query(value = "{'$or':[{ 'name': ?0},{ 'mail': ?1}]}")
  User findOneByNameOrMailWithDeleted(@NotNull String name,@NotNull String mail);

  @Query(value = "{'$and':[{ 'name': ?0},{'deletedAt': null}]}")
  User findByName(@NotNull String name);

  @Query(value = "{'$and':[{ 'id': ?0},{'isAdmin': ?1}]}",count = true)
  long countByIdAndIsAdminWithDeleted(@NotNull String name,boolean admin);

  @Query(value = "{'$and':[{'name':{'$regex':'^?0'}},{'deletedAt': null}]}")
  List<User> findAllByNameStartingWith(@NotNull String start);

  @Query("{ 'name':{'$regex':'^?0'}}")
  List<User> findAllByNameStartingWithWithDeleted(@NotNull String start);

  @Query(value = "{ 'influxBucketName': ?0 }")
  Optional<User> findByInfluxBucketNameWithDeleted(@NotNull String bucketName);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'mail' : ?1 } }")
  void updateMailByUserId(@NotNull String id, String mail);

  Page<User> findAllByDeletedAtBefore(@NotNull LocalDateTime time,@NotNull Pageable pageable);

  @Query("{ '_id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAt(@NotNull String id, LocalDateTime dateTime);
}
