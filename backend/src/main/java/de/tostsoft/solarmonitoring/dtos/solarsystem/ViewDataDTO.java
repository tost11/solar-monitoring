package de.tostsoft.solarmonitoring.dtos.solarsystem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class ViewDataDTO {
  @NotNull
  private Boolean showAmpere;
  private Boolean isBatteryPercentage;
  private Boolean hasACInput;
  private Boolean hasACOutput;
  private Boolean hasDCOutput;
  private Boolean hasTemperature;
  private Boolean productionForTotalPricing;

  @Min(0)
  private Integer voltageAC;

  @Min(0)
  private Integer batteryVoltage;

  @Min(0)
  private Integer maxSolarVoltage;
}
