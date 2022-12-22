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
    protected Float inputWatt;

    protected Float inputVoltageDC;
    protected Float inputAmpereDC;
    protected Float inputWattDC;

    protected Float inputVoltageAC;
    protected Float inputAmpereAC;
    protected Float inputWattAC;

    //output
    protected Float outputWatt;

    protected Float outputVoltageDC;
    protected Float outputAmpereDC;
    protected Float outputWattDC;

    protected Float outputVoltageAC;
    protected Float outputAmpereAC;
    protected Float outputWattAC;

    //frequency
    protected Float outputFrequency;
    protected Float inputFrequency;

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
