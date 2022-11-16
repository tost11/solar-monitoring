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
public class BaseDeviceDTO<INPUT extends BaseInputDTO,OUTPUT extends BaseOutputDTO> {

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

    //total values
    protected Float totalKWH;
    protected Float totalOH;

    @Valid
    protected List<INPUT> inputs;

    @Valid
    protected List<OUTPUT> outputs;
}
