package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class PublicSolarSystemDTO {
    @NotNull
    private String id;

    @NotNull
    private String name;

    private String shortener;

    private String description;

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

    @NotNull
    private List<TagDTO> tags;

    private Float maxInstalledSolarPower;
}
