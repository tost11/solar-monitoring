package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemInformationsDTO {
    private String name;
    private String publicName;
    private String description;

    @Min(0)
    private Float maxInstalledSolarPower;

    @Min(0)
    private Float maxInverterOutputPower;

    @Min(0)
    private Float batteryCapacity;

    private ZonedDateTime buildingDate;

    @Min(0)
    private Float electricityPrice;

    @Min(0)
    private Float electricityPriceFeedIn;
}
