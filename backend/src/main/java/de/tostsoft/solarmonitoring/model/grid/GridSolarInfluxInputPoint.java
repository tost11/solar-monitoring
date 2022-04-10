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
public class GridSolarInfluxInputPoint extends GenericInfluxPoint {

  private Float chargeVoltage;
  private Float chargeAmpere;
  private Float chargeWatt;

  private Long id;
  private Long deviceId;
}
