package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarSystemListItemDTO {
    @NotNull
    private String id;

    private String role;

    private String shortener;

    @NotNull
    private String name;

    @NotNull
    private SolarSystemType type;
}
