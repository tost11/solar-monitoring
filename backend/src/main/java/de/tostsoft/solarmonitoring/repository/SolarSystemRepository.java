package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SolarSystemRepository extends MongoRepository<SolarSystem,String> {

}
