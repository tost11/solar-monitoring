package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RegisterSolarSystemDTO {

    @NotNull
    private String name;

    private String shortener;

    private ZonedDateTime buildingDate;

    @NotNull
    private SolarSystemType type;

    private Float electricityPrice;

    @NotNull
    private ViewDataDTO viewData;

    @Min(0)
    private Integer voltageAC;

    @Min(0)
    private Integer batteryVoltage;

    @Min(0)
    private Integer maxSolarVoltage;

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
