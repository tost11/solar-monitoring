package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GridDeviceDTO {

  @NotNull
  private Long id;

  private Float totalKWH;
  private Float totalOH;

  private Float deviceTemperature;

  private Float chargeVoltage;
  private Float chargeAmpere;
  private Float chargeWatt;

  private Float gridVoltage;
  private Float gridAmpere;
  private Float gridWatt;

  private Float frequency;
  private Float phase;

  @Valid
  private List<GridInputDTO> inputs;
  @Valid
  private List<GridOutputDTO> outputs;
}
