package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import java.util.Date;

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

    private Float latitude;

    private Float longitude;

    private Boolean isBatteryPercentage;

    private Float inverterVoltage;

    private Float batteryVoltage;

    private Float maxSolarVoltage;
}
