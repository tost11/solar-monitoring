package de.tostsoft.solarmonitoring.dtos.solarsystem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfMadeSolarSampleOnlyInverterDTO {

  private Long timestamp;
  private float duration;
  private float systemId;

  private float chargeVolt;
  private float chargeAmpere;
  private Float chargeWatt;
  private Float chargeTemperature;
  //Battery
  private float batteryVoltage;
  private float batteryAmpere;
  private Float batteryWatt;
  private Float batteryPercentage;
  private Float batteryTemperature;
  //Consumption
  private Float consumptionInverterVoltage;
  private float consumptionInverterAmpere;
  private Float consumptionInverterWatt;
  private Float inverterTemperature;

  private Float deviceTemperature;
}