package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SolarDeviceInfluxPoint extends GenericSolarInfluxPoint{

    @NotNull
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
