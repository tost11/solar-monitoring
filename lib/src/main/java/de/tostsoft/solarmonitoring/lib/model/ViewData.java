package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import lombok.*;

import java.time.Duration;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewData {
  private Integer batteryVoltage;
  private Integer maxSolarVoltage;
  private Boolean hasTemperature;
  private Boolean productionForTotalPricing;
  private Boolean totalPricingPublicOverride;
  private Boolean hideTotalConsumption;
  private Boolean showGridInfo;
  private Integer defaultDelay;
  private Set<String> totalFilter;
  private Set<GraphFilter> graphFilter;

  static public Duration DEFAULT_DEFAULTDURATION = Duration.ofMinutes(5);
}
