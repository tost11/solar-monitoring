package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.JWTSessionToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

@Validated
public interface JWTSessionTokenRepository extends MongoRepository<JWTSessionToken,String> {

    void deleteAllByOwnedByIs(@NotNull User owner);

    long countByValidUntilBefore(@NotNull Instant date);
}
