package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

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
public class ShareLinkRedeemResponseDTO {
    private String systemId;
    private String level;
    private String password;
    private LocalDateTime expiresAt;
}
