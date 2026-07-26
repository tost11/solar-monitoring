package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateAccessTokenDTO {
    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    private LocalDateTime expiresAt;

    private boolean regenerateToken;
}
