package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class RegisterSolarSystemDTO {
    @NotNull
    private String name;

    private Date buildingDate;

    @NotNull
    private SolarSystemType type;

    private Double latitude;

    private Double longitude;

    private Boolean isBatteryPercentage;

    private Integer inverterVoltage;

    private Integer batteryVoltage;

    private Integer maxSolarVoltage;
}
