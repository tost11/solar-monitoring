package de.tostsoft.solarmonitoring.model.influx;

import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public abstract class GenericSolarInfluxPoint extends GenericInfluxPoint {

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
    protected Float batteryVoltage;
    protected Float batteryAmpere;
    protected Float batteryWatt;

    protected Float batteryPercentage;
    protected Float batteryTemperature;

    //total values
    protected Float totalOH;

    protected Float inputTotalKWH;
    protected Float outputTotalOH;

    protected Float outputTotalKWH;
    protected Float inputTotalOH;
}
