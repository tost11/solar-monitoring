package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfMadeSolarSampleConsumptionDeviceDTO {

  Long timestamp;
  private Long duration;

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
  private Float consumptionVoltage;
  private float consumptionAmpere;
  private Float consumptionWatt;

  private Float deviceTemperature;
}
