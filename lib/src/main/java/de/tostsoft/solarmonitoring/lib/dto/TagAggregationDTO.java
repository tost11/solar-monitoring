package de.tostsoft.solarmonitoring.lib.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagAggregationDTO {
    private TagDTO tag;
    private int totalSystems;
    private int onlineSystems;
    private float totalDayProducedKWH;
    private float totalDayConsumedKWH;
    private float totalCurrentProduction;
    private float totalCurrentConsumption;
    private float totalCurrentGrid;
    private float totalBatteryRemainingKWH;
    private Float[] combinedProductionCurve;
    private PagedResponse<SystemContributionDTO> systems;
}
