package de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelfMadeSolarSampleConsumptionInverterDTO {

  private Long timestamp;
  @NotNull
  private Float duration;

  @NotNull
  private Float chargeVoltage;
  @NotNull
  private Float chargeAmpere;
  private Float chargeWatt;
  private Float chargeTemperature;
  //Battery
  @NotNull
  private Float batteryVoltage;
  @NotNull
  private Float batteryAmpere;
  private Float batteryWatt;
  private Float batteryPercentage;
  private Float batteryTemperature;

  //Consumption
  private Float consumptionInverterVoltage;
  @NotNull
  private Float consumptionInverterAmpere;
  private Float consumptionInverterWatt;
  private Float inverterTemperature;

  private Float deviceTemperature;
}
