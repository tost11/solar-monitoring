package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ManagesRepository extends MongoRepository<Manages,String> {

  Manages findByUserIdAndSolarSystemIdAndPermissionIn(String userid,String systemId, List<Permissions> permissionsList);

}
