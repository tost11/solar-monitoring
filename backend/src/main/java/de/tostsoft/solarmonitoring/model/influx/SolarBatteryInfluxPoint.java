package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SolarBatteryInfluxPoint extends GenericInfluxPoint {
    private Long id;
    private Long deviceId;

    protected Float voltage;
    protected Float ampere;
    protected Float watt;

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA_BATTERY;
    }
}
