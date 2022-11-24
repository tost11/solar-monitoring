package de.tostsoft.solarmonitoring.model.influx;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SolarBatteryPoint {
    private Long id;
    private Long deviceId;

    protected Float voltage;
    protected Float ampere;
    protected Float watt;

}
