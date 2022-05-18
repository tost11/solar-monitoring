package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarSystemListItemDTO {
    @NotNull
    private Long id;

    private String role;

    @NotNull
    private String name;

    @NotNull
    private SolarSystemType type;
}
