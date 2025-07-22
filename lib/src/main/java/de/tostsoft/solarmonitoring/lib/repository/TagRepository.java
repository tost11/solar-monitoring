package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Tag;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface TagRepository extends MongoRepository<Tag,String> {

    long countByName(@NotNull String name);

    List<Tag> findAllByShowOnStartPage(boolean b);

    List<Tag> findAllByIdIn(@NotNull List<ObjectId> ids);
}
