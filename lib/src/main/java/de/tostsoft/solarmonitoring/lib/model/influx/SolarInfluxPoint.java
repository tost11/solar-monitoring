package de.tostsoft.solarmonitoring.lib.model.influx;

import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SolarInfluxPoint extends GenericSolarInfluxPoint {

    protected Integer numActiveDevices;

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA;
    }
}
