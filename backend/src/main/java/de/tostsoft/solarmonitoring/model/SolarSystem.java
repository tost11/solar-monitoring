package de.tostsoft.solarmonitoring.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Getter
@Setter
@NoArgsConstructor
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Node
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
    private ArrayList<String> labels=new ArrayList();


    private Float latitude;

    private Float longitude;

    private Boolean isBatteryPercentage;

    private Float inverterVoltage;

    private Float batteryVoltage;

    private Float maxSolarVoltage;

    @Lazy
    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private User relationOwnedBy;

    @Lazy
    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<User> relationManageBy;


    public void addLabel(String addLabel){
        labels.add(addLabel);

    }
}
