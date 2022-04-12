package de.tostsoft.solarmonitoring.dtos.solarsystem.data.simple;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleSampleDTO {

  private Long timestamp;

  @NotNull
  private Float duration;

  @NotNull
  private Float voltage;
  @NotNull
  private Float ampere;

  private Float watt;

  private Float deviceTemperature;
}
