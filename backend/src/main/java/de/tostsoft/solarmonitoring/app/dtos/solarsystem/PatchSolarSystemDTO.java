package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PatchSolarSystemDTO{
    @NotNull
    private String id;

    @NotNull
    private String name;

    private String shortener;

    private ZonedDateTime buildingDate;

    @NotNull
    private SolarSystemType type;

    private Float electricityPrice;

    @NotNull
    private ViewDataDTO viewData;

    @NotNull
    private String timezone;

    @NotNull
    private PublicMode publicMode;

    @NotNull
    private NamingsDTO namings;

    @Size(max=1000)
    private String deyeSunSerialNumbers;

    private Boolean calculateCombinedValuesAfterwards;
}
