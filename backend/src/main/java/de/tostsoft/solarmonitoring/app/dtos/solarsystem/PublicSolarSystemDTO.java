package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class PublicSolarSystemDTO {
    @NotNull
    private String id;

    @NotNull
    private String viewName;

    private String shortener;

    @NotNull
    private NamingsDTO namings;

    //is here for graph start selection
    @NotNull
    private ZonedDateTime creationDate;
    private ZonedDateTime buildingDate;

    @NotNull
    private ViewDataDTO viewData;

    private PublicMode publicMode;

    private Boolean publicFlagOnlyProduction;

    @NotNull
    private String timezone;

    @NotNull
    private SolarSystemType type;
}
