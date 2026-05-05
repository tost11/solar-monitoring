package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.AccountLockout;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface AccountLockoutRepository extends MongoRepository<AccountLockout, String> {

    Optional<AccountLockout> findByUsername(String username);

    long countByLockedUntilAfter(Instant now);

    long countByExpiresAtBefore(Instant timestamp);
}
