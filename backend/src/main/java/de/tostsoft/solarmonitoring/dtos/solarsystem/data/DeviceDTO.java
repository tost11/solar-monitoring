package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceDTO {

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
    protected Float inputTotalKWH;
    protected Float outputTotalKWH;
    protected Float totalOH;

    @Valid
    protected List<InputDTO> inputs;

    @Valid
    protected List<OutputDTO> outputs;

    @Valid
    protected List<BatteryDTO> batteries;
}
