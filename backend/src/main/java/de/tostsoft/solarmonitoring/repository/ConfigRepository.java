package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.Config;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface  ConfigRepository  extends Neo4jRepository<Config, Long> {

  public Config findByName(String name);

  @Query("CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (c:Config) ASSERT c.name IS UNIQUE")
  void initNameConstrain();

  @Query("MATCH (c:Config) WHERE c.name = $name SET c.isRegistrationEnabled = $enabled")
  void setRegistrationEnabled(String name,boolean enabled);
}
