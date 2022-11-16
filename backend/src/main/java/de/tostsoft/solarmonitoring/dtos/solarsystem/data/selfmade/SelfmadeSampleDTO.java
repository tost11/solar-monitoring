package de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade;

import javax.validation.constraints.NotNull;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseSampleDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfmadeSampleDTO extends BaseSampleDTO<SelfmadeDeviceDTO> {

  @NotNull
  private Float batteryVoltage;
  private Float batteryAmpere;
  private Float batteryWatt;

  private Float batteryPercentage;
  private Float batteryTemperature;
}
