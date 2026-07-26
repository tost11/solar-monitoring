package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreatedAccessTokenResponseDTO {
    private String id;
    private String name;
    private TokenPurpose purpose;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String token;
}
