package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SolarOutputACInfluxPoint extends GenericInfluxPoint {

  private Long id;
  private Long deviceId;

  protected Float voltage;
  protected Float ampere;
  protected Float watt;

  protected Float frequency;
  private Integer phase;

  @Override
  public InfluxMeasurement getMeasurement() {
    return InfluxMeasurement.SOLAR_DATA_OUTPUT_AC;
  }
}
