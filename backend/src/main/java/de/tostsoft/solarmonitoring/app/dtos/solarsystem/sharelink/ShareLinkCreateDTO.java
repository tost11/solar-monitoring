package de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class ShareLinkCreateDTO {
    private String secret;

    @NotNull
    @Pattern(regexp = "ALL|PRODUCTION", message = "level must be ALL or PRODUCTION")
    private String level;

    @Min(value = 1, message = "maxUses must be positive")
    private Integer maxUses;

    @Future(message = "expiresAt must be in the future")
    private LocalDateTime expiresAt;

    @Future(message = "tokenExpiresAt must be in the future")
    private LocalDateTime tokenExpiresAt;

    private String notes;
}
