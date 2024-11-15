package de.tostsoft.solarmonitoring.whatever.repository;

import de.tostsoft.solarmonitoring.whatever.model.ProxySolarSystem;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface ProxySolarSystemRepository extends MongoRepository<ProxySolarSystem,String> {

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'token' : ?1 } }")
  void updateToken(String id, String token);

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'deyeSunSerials' : ?1 } }")
  void updateDeyeSerials(String id, Set<Long> serials);

  void deleteAllByIdNotIn(List<String> ids);
}
