package de.tostsoft.solarmonitoring.proxy.repository;

import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSystem;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface ProxySolarSystemRepository extends MongoRepository<ProxySolarSystem,String> {

  @Query("{ '_id' : ?0 }")
  @Update("{ '$set' : { 'deyeSunSerials' : ?1 } }")
  void updateDeyeSerials(String id, Set<Long> serials);

  void deleteAllByIdNotIn(List<String> ids);

  @Query(value = "{deyeSunSerials:{$elemMatch:{$eq:?0}}}")
  Optional<ProxySolarSystem> findSolarSystemBySerialInAndDeyeSunSerials(Long serial);

}
