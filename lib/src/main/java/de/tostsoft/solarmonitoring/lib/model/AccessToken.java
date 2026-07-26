package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessToken {
    private String id;
    private String name;
    private String hash;
    private TokenPurpose purpose;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
