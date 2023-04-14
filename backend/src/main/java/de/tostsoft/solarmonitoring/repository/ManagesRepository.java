package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.Manages;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ManagesRepository extends MongoRepository<Manages,String> {

}
