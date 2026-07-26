package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateAccessTokenDTO {
    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @NotNull
    private TokenPurpose purpose;

    private LocalDateTime expiresAt;
}
