package de.tostsoft.solarmonitoring.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "loginAttempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    private String id;

    @Indexed
    private String username;

    @Indexed
    private String ipAddress;

    private boolean success;

    @Indexed(expireAfterSeconds = 86400)
    private Instant timestamp;

    private String userAgent;
}
