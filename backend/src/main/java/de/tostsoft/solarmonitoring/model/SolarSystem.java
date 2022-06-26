package de.tostsoft.solarmonitoring.model;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Relationship;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SolarSystem {
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

    private Integer inverterVoltage;

    private Integer batteryVoltage;

    private Integer maxSolarVoltage;

    private PublicMode publicMode;

    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private User relationOwnedBy;

    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<ManageBY> relationManageBy;

    private String timezone;

    private ZonedDateTime lastCalculation;
}
