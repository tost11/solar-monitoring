package de.tostsoft.solarmonitoring.app.dtos.solarsystem.token;

import de.tostsoft.solarmonitoring.lib.model.ViewPermissionInfo;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AccessTokenResponseDTO {
    private String id;
    private String name;
    private TokenPurpose purpose;
    private ViewPermissionInfo viewPermission;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
