package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleDTO {

    @Min(value = 0)
    protected Long timestamp;

    @NotNull
    @Min(value = 0)
    protected Float duration;

    protected Float temperature;

    //input
    @Min(value = 0)
    protected Float inputWatt;

    @Min(value = 0)
    protected Float inputVoltageDC;
    @Min(value = 0)
    protected Float inputAmpereDC;
    @Min(value = 0)
    protected Float inputWattDC;

    @Min(value = 0)
    protected Float inputVoltageAC;
    @Min(value = 0)
    protected Float inputAmpereAC;
    @Min(value = 0)
    protected Float inputWattAC;

    //output
    @Min(value = 0)
    protected Float outputWatt;
    
    @Min(value = 0)
    protected Float outputVoltageDC;
    @Min(value = 0)
    protected Float outputAmpereDC;
    @Min(value = 0)
    protected Float outputWattDC;

    @Min(value = 0)
    protected Float outputVoltageAC;
    @Min(value = 0)
    protected Float outputAmpereAC;
    @Min(value = 0)
    protected Float outputWattAC;

    //frequency
    @Min(value = 0)
    protected Float outputFrequency;
    @Min(value = 0)
    protected Float inputFrequency;

    //battery
    @Min(value = 0)
    private Float batteryVoltage;
    private Float batteryAmpere;
    private Float batteryWatt;

    @Min(value = 0)
    @Max(value = 100)
    private Float batteryPercentage;
    private Float batteryTemperature;

    //total values
    @Min(value = 0)
    protected Float totalOH;

    @Min(value = 0)
    protected Float inputTotalKWH;
    @Min(value = 0)
    protected Float outputTotalOH;

    @Min(value = 0)
    protected Float outputTotalKWH;
    @Min(value = 0)
    protected Float inputTotalOH;

    @Valid
    protected List<DeviceDTO> devices;
}
