package de.tostsoft.solarmonitoring.lib.model.influx;

import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    protected Float totalKWH;

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA_BATTERY;
    }
}
