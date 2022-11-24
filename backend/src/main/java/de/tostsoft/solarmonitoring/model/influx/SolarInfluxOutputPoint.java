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
public class SolarInfluxOutputPoint extends GenericInfluxPoint {

  private Long id;
  private Long deviceId;

  protected Float voltage;
  protected Float ampere;
  protected Float watt;

  protected Float frequency;
  private Integer phase;
}
