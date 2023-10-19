package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MultSolarSystemDTO {

    @NotNull
    private String id;

    @NotNull
    private String name;
    @NotNull
    private String viewName;

    private String shortner;

    @NotNull
    private SolarSystemType type;

    private PublicMode publicMode;
}