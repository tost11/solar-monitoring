package de.tostsoft.solarmonitoring.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Relationship;

@Getter
@Setter
@NoArgsConstructor
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class SolarSystem {
    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    private String name;

    private String token;
    @NotNull
    private LocalDateTime creationDate;
    private LocalDateTime buildingDate;
    private SolarSystemType type;

    @DynamicLabels
    private Set<String> labels;

    private Double latitude;

    private Double longitude;

    private Boolean isBatteryPercentage;

    private Integer inverterVoltage;

    private Integer batteryVoltage;

    private Integer maxSolarVoltage;

    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private User relationOwnedBy;

    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<ManageBY> relationManageBy;
}
