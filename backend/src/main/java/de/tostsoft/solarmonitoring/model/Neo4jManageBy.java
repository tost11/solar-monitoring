package de.tostsoft.solarmonitoring.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@Getter
@Setter
@RelationshipProperties
@NoArgsConstructor
@AllArgsConstructor
public class Neo4jManageBy {
    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Neo4jUser neo4jUser;

    @NotNull
    private Permissions permission;

    public Neo4jManageBy(Neo4jUser neo4jUser,Permissions permission){
        this.neo4jUser = neo4jUser;
        this.permission = permission;
    }

}
