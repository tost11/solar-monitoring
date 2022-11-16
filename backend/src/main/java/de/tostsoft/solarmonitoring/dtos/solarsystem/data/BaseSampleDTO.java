package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseSampleDTO<DEVICE extends BaseDeviceDTO> {

    protected Long timestamp;
    @NotNull
    protected Float duration;

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

    //total values
    protected Float totalOH;

    protected Float inputTotalKWH;
    protected Float outputTotalOH;

    protected Float outputTotalKWH;
    protected Float inputTotalOH;

    @Valid
    protected List<DEVICE> devices;
}
