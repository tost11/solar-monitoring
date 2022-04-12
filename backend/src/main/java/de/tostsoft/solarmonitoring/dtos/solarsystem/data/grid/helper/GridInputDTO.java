package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper;

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
public class GridInputDTO {
  @NotNull
  private Long id;
  @NotNull
  private Float voltage;
  @NotNull
  private Float ampere;
  private Float watt;
}
