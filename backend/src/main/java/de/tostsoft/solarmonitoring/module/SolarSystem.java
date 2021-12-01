package de.tostsoft.solarmonitoring.module;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import javax.naming.Name;
import java.util.*;

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
    private String type;

    private Float latitude;

    private Float longitude;

    @Relationship(type = "owns",direction = Relationship.Direction.INCOMING)
    private List<SolarSystem> relationOwns;

    @Relationship(type = "manageBy",direction = Relationship.Direction.OUTGOING)
    private List<SolarSystem> manageBy;



}
