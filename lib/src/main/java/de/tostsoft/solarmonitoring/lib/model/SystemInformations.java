package de.tostsoft.solarmonitoring.lib.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemInformations {
    @Indexed
    @NotNull
    private String name;
    @NotNull
    private String viewName;
    private String publicName;
    private String description;
    private Float maxInstalledSolarPower;
    private Float maxInverterOutputPower;
    private Float batteryCapacity;
    private LocalDateTime buildingDate;
    private Float electricityPrice;
    private Float electricityPriceFeedIn;
}
