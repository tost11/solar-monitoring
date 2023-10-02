package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewData {
  private Boolean isBatteryPercentage;
  private Boolean hasACInput;
  private Boolean hasDCOutput;
  private Boolean hasACOutput;
  private Boolean showAmpere;
  private Integer voltageAC;
  private Integer batteryVoltage;
  private Integer maxSolarVoltage;
  private Boolean hasTemperature;
  private Boolean productionForTotalPricing;
  private Boolean totalPricingPublicOverride;
  private Boolean hideTotalConsumption;
}
