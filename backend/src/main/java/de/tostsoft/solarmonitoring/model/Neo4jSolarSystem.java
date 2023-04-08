package de.tostsoft.solarmonitoring.model;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Node("SolarSystem")
public class Neo4jSolarSystem {
    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    private String name;

    private String token;
    @NotNull
    private ZonedDateTime creationDate;
    private ZonedDateTime buildingDate;
    private SolarSystemType type;

    @DynamicLabels
    private Set<String> labels;

    private Double latitude;

    private Double longitude;

    private Boolean isBatteryPercentage;
    private Boolean hasACInput;
    private Boolean hasDCOutput;
    private Boolean hasACOutput;
    private Boolean showAmpere;

    private Integer voltageAC;

    private Integer batteryVoltage;

    private Integer maxSolarVoltage;

    private PublicMode publicMode;

    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private Neo4jUser relationOwnedBy;

    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<Neo4jManageBy> relationNeo4jManageBy;

    private String timezone;

    private ZonedDateTime lastCalculation;
    private ZonedDateTime lastManualCalculation;
}
