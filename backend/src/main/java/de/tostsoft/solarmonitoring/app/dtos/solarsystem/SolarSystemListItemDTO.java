package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    private CurrentValuesDTO currentValues;
    private Float totalProducedWH;
    private List<TagDTO> tags;
    private Float maxInstalledSolarPower;
    private Float maxInverterOutputPower;
    private Float batteryCapacity;
    private LocalDateTime buildingDate;
    private LocalDateTime creationDate;
}
