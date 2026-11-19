package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ViewTokenValidateDTO {
    @NotBlank
    private String systemId;

    @NotBlank
    private String password;
}
