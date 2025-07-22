package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Manages;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ManagesRepository extends SoftDeleteMongoRepository<Manages,String> {

  //------------ other funkction -----------------
  @Query("{ 'user._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllRelationByUser(@NotNull String id, @NotNull LocalDateTime dateTime);

  @Query("{ 'solarSystem._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllRelationBySolarSystem(@NotNull String id,@NotNull LocalDateTime dateTime);


}
