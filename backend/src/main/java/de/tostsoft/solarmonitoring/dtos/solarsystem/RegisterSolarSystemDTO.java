package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterSolarSystemDTO {
    @NotNull
    private String name;

    private ZonedDateTime buildingDate;

    @NotNull
    private SolarSystemType type;

    private Double latitude;

    private Double longitude;

    private Boolean isBatteryPercentage;

    private Integer inverterVoltage;

    private Integer batteryVoltage;

    @NotNull
    private Integer maxSolarVoltage;

    @NotNull
    private String timezone;

    @NotNull
    private PublicMode publicMode;
}
