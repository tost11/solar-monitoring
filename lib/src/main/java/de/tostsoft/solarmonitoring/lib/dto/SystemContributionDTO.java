package de.tostsoft.solarmonitoring.lib.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemContributionDTO {
    private String id;
    private String name;
    private String type;
    @JsonProperty("isOnline")
    private boolean isOnline;
    private float dayProducedKWH;
    private Float dayConsumedKWH;
    private float dayProductionPercentage;
    private Float dayConsumptionPercentage;
    private float currentProduction;
    private Float currentConsumption;
    private float currentProductionPercentage;
    private Float currentConsumptionPercentage;
    private Float currentGrid;
    private String role;
}
