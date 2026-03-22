package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ViewSolarSystemDTO extends PublicSolarSystemDTO {
    //TODO check why this values are here (and are they needed here?)
    private Float electricityPrice;
    private Float electricityPriceFeedIn;
}
