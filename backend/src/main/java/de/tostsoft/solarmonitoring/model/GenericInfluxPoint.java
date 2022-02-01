package de.tostsoft.solarmonitoring.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GenericInfluxPoint {

    private Long timestamp;
    private SolarSystemType type;
}
