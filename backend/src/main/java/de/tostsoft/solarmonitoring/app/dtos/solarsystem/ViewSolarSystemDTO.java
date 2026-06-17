package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ViewSolarSystemDTO extends PublicSolarSystemDTO {
    private Float electricityPrice;
    private Float electricityPriceFeedIn;
    private Float maxInverterOutputPower;
    private Float batteryCapacity;
}
