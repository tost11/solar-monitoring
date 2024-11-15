package de.tostsoft.solarmonitoring.whatever.repository;

import de.tostsoft.solarmonitoring.whatever.model.ProxySolarSample;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProxySolarSampleRepository extends MongoRepository<ProxySolarSample,String> {

}
