package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

@Document("solarSystem-deleted")
public interface DeletedSolarSystemRepository extends MongoRepository<SolarSystem,String> {
}
