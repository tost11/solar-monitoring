package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;

//TODO split in two DTOs for creation and getting
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RegisterSolarSystemResponseDTO {

    @NotNull
    private String id;
    @NotNull
    private String token;
    @NotNull
    private String name;
    @NotNull
    private String viewName;
    private String shortener;
    @NotNull
    private ZonedDateTime creationDate;
    private ZonedDateTime buildingDate;
    @NotNull
    private SolarSystemType type;

    @NotNull
    private ViewDataDTO viewData;

    private Float electricityPrice;
    private Float electricityPriceFeedIn;

    private PublicMode publicMode;

    @NotNull
    private String timezone;

    @NotNull
    private NamingsDTO namings;

    private String deyeSunSerialNumbers;

    private Boolean calculateCombinedValuesAfterwards;
}