package de.tostsoft.solarmonitoring.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "accountLockouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockout {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed
    private Instant lockedUntil;

    private Instant lockedAt;

    private int failedAttempts;

    private String lastAttemptIp;

    @Indexed(expireAfterSeconds = 604800)
    private Instant expiresAt;
}
