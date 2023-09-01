package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.status.AllStatusResponseDTO;
import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

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