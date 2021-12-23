package de.tostsoft.solarmonitoring.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Node("SolarSystem")
public class SolarSystem {
    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    private String token;
    @NotNull
    private String name;
    @NotNull
    private Date creationDate;
    @NotNull
    private SolarSystemType type;

    private Float latitude;

    private Float longitude;

    @Lazy
    @Relationship(type = "owns", direction = Relationship.Direction.INCOMING)
    private User relationOwnedBy;

    @Lazy
    @Relationship(type = "manages", direction = Relationship.Direction.INCOMING)
    private List<User> relationManageBy;


}
