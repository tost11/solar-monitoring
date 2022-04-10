package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper;

import java.util.List;
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

  @NotNull
  List<GridInputDTO> inputs;
  @NotNull
  List<GridOutputDTO> outputs;
}
