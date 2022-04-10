package de.tostsoft.solarmonitoring.model.grid;

import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GridSolarInfluxOutputPoint extends GenericInfluxPoint {

  private Float gridVoltage;
  private Float gridAmpere;
  private Float gridWatt;

  private Float frequency;
  private Integer phase;

  private Long id;
  private Long deviceId;
}
