package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridInputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridOutputDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputOutputGridSolarSampleDTO {
  private Long timestamp;
  @NotNull
  private Float duration;

  private Float totalKWH;
  private Float totalOH;

  private Float chargeVoltage;
  private Float chargeAmpere;
  private Float chargeWatt;

  private Float gridVoltage;
  private Float gridAmpere;
  private Float gridWatt;

  private Float frequency;

  private Float deviceTemperature;

  @NotNull
  List<GridInputDTO> inputs;
  @NotNull
  List<GridOutputDTO> outputs;
}
