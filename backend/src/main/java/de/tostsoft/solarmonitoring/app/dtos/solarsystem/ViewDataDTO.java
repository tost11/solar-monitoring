package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewDataDTO {
  private Boolean hasTemperature;
  private Boolean productionForTotalPricing;
  private Boolean totalPricingPublicOverride;
  private Boolean hideTotalConsumption;
  private Boolean showGridInfo;

  @Min(0)
  private Integer voltageAC;

  @Min(0)
  private Integer batteryVoltage;

  @Min(0)
  private Integer maxSolarVoltage;

  @Min(0)
  private Integer defaultDelay;

  private Set<String> totalFilter;
  private Set<GraphFilter> graphFilter;
}
