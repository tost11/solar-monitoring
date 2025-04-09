package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ViewSolarSystemDTO extends PublicSolarSystemDTO {

    private Double latitude;

    private Double longitude;

    private Float electricityPrice;
}
