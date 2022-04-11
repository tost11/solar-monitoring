package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper;

import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GridDeviceDTO {

  @NotNull
  Long id;

  Float totalKWH;
  Float totalOH;

  Float deviceTemperature;

  private Float chargeVoltage;
  private Float chargeAmpere;
  private Float chargeWatt;

  private Float gridVoltage;
  private Float gridAmpere;
  private Float gridWatt;

  Float frequency;
  Float phase;

  @Valid
  List<GridInputDTO> inputs;
  @Valid
  List<GridOutputDTO> outputs;
}
