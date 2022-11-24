package de.tostsoft.solarmonitoring.model.influx;

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
public class SolarInfluxInputPoint extends GenericInfluxPoint {

  private Long id;
  private Long deviceId;

  private Float voltage;
  private Float ampere;
  private Float watt;

}
