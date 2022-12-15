package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceDTO {

    @NotNull
    @Min(value = 0)
    protected Long id;

    protected Float temperature;

    //input
    @Min(value = 0)
    protected Float inputVoltage;
    @Min(value = 0)
    protected Float inputAmpere;
    @Min(value = 0)
    protected Float inputWatt;

    //output
    @Min(value = 0)
    protected Float outputVoltage;
    @Min(value = 0)
    protected Float outputAmpere;
    @Min(value = 0)
    protected Float outputWatt;

    @Min(value = 0)
    protected Float frequency;

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
    protected Float inputTotalKWH;
    @Min(value = 0)
    protected Float outputTotalKWH;
    @Min(value = 0)
    protected Float totalOH;

    @Valid
    protected List<InputDTO> inputs;

    @Valid
    protected List<OutputDTO> outputs;

    @Valid
    protected List<BatteryDTO> batteries;
}
