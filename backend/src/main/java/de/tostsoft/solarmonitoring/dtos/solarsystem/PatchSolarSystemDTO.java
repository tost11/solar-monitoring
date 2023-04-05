package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.Min;
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
    private Long id;

    @NotNull
    private String name;

    private ZonedDateTime buildingDate;

    @NotNull
    private SolarSystemType type;

    private Double latitude;
    private Double longitude;

    private Boolean showAmpere;
    private Boolean isBatteryPercentage;
    private Boolean hasACInput;
    private Boolean hasACOutput;
    private Boolean hasDCOutput;

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
}
