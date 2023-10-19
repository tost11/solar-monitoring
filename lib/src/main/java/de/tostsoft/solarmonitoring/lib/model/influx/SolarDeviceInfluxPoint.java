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
public class SolarDeviceInfluxPoint extends GenericSolarInfluxPoint {

    protected Long id;

    //maby implement this here if it is needed later
    /*
    protected int numActiveDCInputs;
    protected int numaActiveACInputs;
    protected int numaActiveInputs;
    protected int numActiveOutputs;
    protected int numaActiveACOutputs;
    protected int numActiveBatteries;
    */
    protected Integer numActiveConnections;

    @Override
    public InfluxMeasurement getMeasurement() {
        return InfluxMeasurement.SOLAR_DATA_DEVICE;
    }
}
