package de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ShareLinkResponseDTO {
    private String id;
    private String secret;
    private boolean manual;
    private String notes;
    private String level;
    private Integer remainingUsages;
    private LocalDateTime expiresAt;
    private LocalDateTime tokenExpiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;
    private LocalDateTime lastRedeemedAt;
}
