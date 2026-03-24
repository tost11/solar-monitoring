package de.tostsoft.solarmonitoring.lib.model.influx;

import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SolarGridInfluxPoint extends GenericInfluxPoint {
    private Long id;
    private Long deviceId;

    protected Float voltage;
    protected Float ampere;
    protected Float watt;
    protected Float frequency;

    protected Float dailyConsumptionKWH;
    protected Float dailyFeedInKWH;

    protected Float totalConsumptionKWH;
    protected Float totalFeedInKWH;

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA_GRID;
    }
}
