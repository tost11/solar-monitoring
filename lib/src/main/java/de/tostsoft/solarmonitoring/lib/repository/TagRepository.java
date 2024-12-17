package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Tag;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TagRepository extends MongoRepository<Tag,String> {

    long countByName(String name);

    List<Tag> findAllByShowOnStartPage(boolean b);

    List<Tag> findAllByIdIn(List<ObjectId> ids);
}
