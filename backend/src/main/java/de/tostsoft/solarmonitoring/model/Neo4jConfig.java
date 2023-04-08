package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Node("Config")
public class Neo4jConfig {

  @Id
  @GeneratedValue
  private long id;

  private String name;

  private Boolean isRegistrationEnabled;

  @Override
  public String toString() {
    return "Config{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", isRegistrationEnabled=" + isRegistrationEnabled +
        '}';
  }
}
