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
@Setter
@Getter
public class SolarInInputDCInfluxPoint extends GenericInfluxPoint {

  private Long id;
  private Long deviceId;

  private Float voltage;
  private Float ampere;
  private Float watt;

  protected Float totalKWH;

  @Override
  public InfluxMeasurement getMeasurement() {
    return InfluxMeasurement.SOLAR_DATA_INPUT_DC;
  }
}
