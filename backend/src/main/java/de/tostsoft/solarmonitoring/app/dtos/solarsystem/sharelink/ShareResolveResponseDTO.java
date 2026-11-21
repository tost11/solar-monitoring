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
public class ShareResolveResponseDTO {
    private boolean valid;
    private String systemId;
    private String level;
    private String name;
    private LocalDateTime expiresAt;
    private String password;
}
