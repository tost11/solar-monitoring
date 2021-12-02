package de.tostsoft.solarmonitoring.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GenericInfluxPoint {
    public enum InfliuxSolarMeasurement{
        SELFMADE,
        SELFMADE_INVERTER
    };
    private Long timeStep;
    private InfliuxSolarMeasurement measurement;
}
