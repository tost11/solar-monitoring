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
        SELFMADE_CONSUMPTION,
        SELFMADE_INVERTER,
        SELFMADE_DEVICE
    };
    private Long timestamp;
    private InfliuxSolarMeasurement type;
}
