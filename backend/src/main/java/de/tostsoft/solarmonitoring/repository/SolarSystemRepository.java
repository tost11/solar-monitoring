package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {
    Boolean existsAllByToken(String token);

    SolarSystem findByToken(String token);

    SolarSystem findById(long id);

    void deleteByToken(String token);

    boolean existsByName(String name);

    List<SolarSystem> findAllByType(SolarSystemType type);
}
