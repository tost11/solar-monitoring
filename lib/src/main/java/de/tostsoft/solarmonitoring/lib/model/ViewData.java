package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;

import java.time.Duration;
import java.util.Set;

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
  private Integer defaultDelay;
  private Set<String> totalFilter;

  static public Duration DEFAULT_DEFAULTDURATION = Duration.ofMinutes(5);
}
