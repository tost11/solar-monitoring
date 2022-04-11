package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SimpleGridSolarSampleDTO {

  private Long timestamp;
  @NotNull
  private Float duration;

  Float totalKWH;
  Float totalOH;

  @NotNull
  private Float chargeVoltage;
  @NotNull
  private Float chargeAmpere;
  private Float chargeWatt;

  @NotNull
  private Float gridVoltage;
  @NotNull
  private Float gridAmpere;
  private Float gridWatt;
  private Float frequency;
  private Integer phase;

  private Float deviceTemperature;
}
