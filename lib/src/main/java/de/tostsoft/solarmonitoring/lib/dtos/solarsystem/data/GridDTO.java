package de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class GridDTO {
    @NotNull
    @Min(value = 0)
    protected Long id;

    // Real-time metrics
    @Min(value = 0)
    protected Float voltage;
    protected Float ampere;
    protected Float watt;

    // Daily cumulative (reset daily by client)
    @Min(value = 0)
    protected Float dailyConsumption;
    @Min(value = 0)
    protected Float dailyFeedIn;

    // Total cumulative (lifetime counters)
    @Min(value = 0)
    protected Float totalConsumptionKWH;
    @Min(value = 0)
    protected Float totalFeedInKWH;

    @Min(value = 0)
    protected Float frequency;
}
