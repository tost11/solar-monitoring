package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

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
