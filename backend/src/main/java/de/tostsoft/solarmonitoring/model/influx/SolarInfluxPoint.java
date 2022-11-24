package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SolarInfluxPoint extends GenericInfluxPoint {

    @NotNull
    protected Long id;

    protected Float temperature;

    //input
    protected Float inputVoltage;
    protected Float inputAmpere;
    protected Float inputWatt;

    //output
    protected Float outputVoltage;
    protected Float outputAmpere;
    protected Float outputWatt;

    protected Float frequency;

    //battery
    private Float batteryVoltage;
    private Float batteryAmpere;
    private Float batteryWatt;

    private Float batteryPercentage;
    private Float batteryTemperature;

    //total values
    protected Float totalOH;

    protected Float inputTotalKWH;
    protected Float outputTotalOH;

    protected Float outputTotalKWH;
    protected Float inputTotalOH;
}
