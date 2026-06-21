package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemInformationsDTO {
    @NotNull
    private String name;
    private String publicName;

    @Size(max = 300, message = "Description cannot exceed 300 characters")
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
