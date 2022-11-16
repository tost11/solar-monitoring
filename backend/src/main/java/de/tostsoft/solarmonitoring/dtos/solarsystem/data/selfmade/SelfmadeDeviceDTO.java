package de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseInputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseOutputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseSampleDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfmadeDeviceDTO extends BaseDeviceDTO<BaseInputDTO, BaseOutputDTO> {

    @NotNull
    private Float batteryVoltage;
    private Float batteryAmpere;
    private Float batteryWatt;

    private Float batteryPercentage;
    private Float batteryTemperature;
}
