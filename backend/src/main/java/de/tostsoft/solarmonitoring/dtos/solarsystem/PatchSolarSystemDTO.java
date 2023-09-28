package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;

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

    private Double latitude;
    private Double longitude;

    private Float electricityPrice;

    @NotNull
    private ViewDataDTO viewData;

    @NotNull
    private String timezone;

    @NotNull
    private PublicMode publicMode;

    @NotNull
    private NamingsDTO namings;
}
