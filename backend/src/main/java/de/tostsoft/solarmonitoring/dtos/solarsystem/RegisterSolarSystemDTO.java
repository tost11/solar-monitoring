package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RegisterSolarSystemDTO {

    @NotNull
    private String name;

    private ZonedDateTime buildingDate;

    @NotNull
    private SolarSystemType type;

    private Double latitude;
    private Double longitude;

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
}
