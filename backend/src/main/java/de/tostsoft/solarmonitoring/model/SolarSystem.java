package de.tostsoft.solarmonitoring.model;

import java.time.Instant;
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
    private Instant creationDate;
    private Instant buildingDate;
    private SolarSystemType type;

    private Long grafanaId;


    @DynamicLabels
    private Set<String> labels;

    private Float latitude;

    private Float longitude;

    private Boolean isBatteryPercentage;

    private Float inverterVoltage;

    private Float batteryVoltage;

    private Float maxSolarVoltage;

    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private User relationOwnedBy;

    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<User> relationManageBy;

    public void addLabel(String addLabel){
        labels.add(addLabel);
    }
}
