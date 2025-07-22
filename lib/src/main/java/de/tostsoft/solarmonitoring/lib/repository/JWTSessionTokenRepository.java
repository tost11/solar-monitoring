package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.JWTSessionToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Validated
public interface JWTSessionTokenRepository extends MongoRepository<JWTSessionToken,String> {

    void deleteAllByOwnedByIs(@NotNull User owner);

    void deleteAllByValidUntilBefore(@NotNull LocalDateTime date);
}
