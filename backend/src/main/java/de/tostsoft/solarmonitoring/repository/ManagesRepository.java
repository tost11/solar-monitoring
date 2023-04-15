package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ManagesRepository extends MongoRepository<Manages,String> {

  Manages findByUserIdAndSolarSystemIdAndPermissionIn(String userid,String systemId, List<Permissions> permissionsList);

}
