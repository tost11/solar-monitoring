package de.tostsoft.solarmonitoring.dtos.solarsystem.data.simple;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerySimpleSampleDTO {

  private Long timestamp;

  @NotNull
  private Float duration;

  @NotNull
  private Float watt;

  private Float deviceTemperature;
}
