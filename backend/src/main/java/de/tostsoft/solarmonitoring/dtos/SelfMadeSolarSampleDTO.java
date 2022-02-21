package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfMadeSolarSampleDTO {

  private Long timestamp;
  private float duration;

  private float chargeVoltage;
  private float chargeAmpere;
  private Float chargeWatt;
  private Float chargeTemperature;
  //Battery
  private float batteryVoltage;
  private float batteryAmpere;
  private Float batteryWatt;
  private Float batteryPercentage;
  private Float batteryTemperature;

  private Float deviceTemperature;
}
