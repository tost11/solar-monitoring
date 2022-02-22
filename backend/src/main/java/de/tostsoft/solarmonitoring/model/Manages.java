package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Getter
@Setter
@RequiredArgsConstructor
@RelationshipProperties
public class Maneges{
    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @TargetNode
    private SolarSystem solarSystem;

    @NotNull
    private Permissions permissions;



}
