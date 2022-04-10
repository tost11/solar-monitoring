package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper;

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
public class GridOutputDTO {
  @NotNull
  private Long id;
  @NotNull
  private Float voltage;
  @NotNull
  private Float ampere;
  private Float watt;
  private Float frequency;
  private Integer phase;
}
