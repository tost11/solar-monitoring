package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarSystemListItemDTO {
    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private SolarSystemType type;
}
