package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewDataDTO {
  @NotNull
  private Boolean showAmpere;
  private Boolean isBatteryPercentage;
  private Boolean hasACInput;
  private Boolean hasACOutput;
  private Boolean hasDCOutput;
  private Boolean hasTemperature;
  private Boolean productionForTotalPricing;
  private Boolean totalPricingPublicOverride;
  private Boolean hideTotalConsumption;

  @Min(0)
  private Integer voltageAC;

  @Min(0)
  private Integer batteryVoltage;

  @Min(0)
  private Integer maxSolarVoltage;

  @Min(0)
  private Integer defaultDelay;

  private Set<String> totalFilter;
}
