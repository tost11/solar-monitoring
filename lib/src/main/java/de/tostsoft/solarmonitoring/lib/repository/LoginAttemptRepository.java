package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.LoginAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends MongoRepository<LoginAttempt, String> {

    long countByUsernameAndTimestampAfterAndSuccessFalse(String username, Instant since);

    long countByIpAddressAndTimestampAfterAndSuccessFalse(String ipAddress, Instant since);

    List<LoginAttempt> findByUsernameAndTimestampAfter(String username, Instant since);

    long countBySuccessFalse();

    long countBySuccessTrue();
}
