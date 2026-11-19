package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.ShareLink;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends MongoRepository<ShareLink, String> {

    List<ShareLink> findBySystemId(String systemId);

    Optional<ShareLink> findByIdAndSystemId(String id, String systemId);

    void deleteBySystemId(String systemId);

    @Query("{ '_id' : ?0, 'remainingUsages' : { '$gt' : 0 } }")
    @Update("{ '$inc' : { 'remainingUsages' : -1 } }")
    int decrementRemainingUsages(@NotNull String id);
}
