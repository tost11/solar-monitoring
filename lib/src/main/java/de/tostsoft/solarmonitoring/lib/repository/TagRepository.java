package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagRepository extends MongoRepository<Tag,String> {

    long countByName(String name);
}
