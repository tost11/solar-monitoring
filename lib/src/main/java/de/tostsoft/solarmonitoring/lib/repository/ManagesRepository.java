package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ManagesRepository extends MongoRepository<Manages,String> {

  Manages findByUserIdAndSolarSystemIdAndPermissionIn(String userid,String systemId, List<Permissions> permissionsList);

  void deleteAllByUserIs(User user);

  @Query("{ 'user._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllRelationByUser(String id, LocalDateTime dateTime);

  @Query("{ 'solarSystem._id': ?0}")
  @Update("{ '$set' : { 'deletedAt' : ?1 } }")
  void setDeleteAtOnAllRelationBySolarSystem(String id, LocalDateTime dateTime);
}
