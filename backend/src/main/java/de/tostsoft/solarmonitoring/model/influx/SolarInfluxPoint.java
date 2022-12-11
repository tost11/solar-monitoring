package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder()
public class SolarInfluxPoint extends GenericSolarInfluxPoint{

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA;
    }
}
