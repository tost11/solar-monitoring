package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.JWTSessionToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface JWTSessionTokenRepository extends MongoRepository<JWTSessionToken,String> {

    void deleteAllByOwnedByIs(User owner);

    void deleteAllByValidUntilBefore(LocalDateTime date);
}
