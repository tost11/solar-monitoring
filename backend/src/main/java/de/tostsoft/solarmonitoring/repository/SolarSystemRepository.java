package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import de.tostsoft.solarmonitoring.model.User;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {
    Boolean existsAllByToken(String token);

    SolarSystem findById(long id);
    boolean existsById(long id);

    boolean existsByName(String name);

    SolarSystem findByIdAnAndRelationOwnedById(long systemId,long userId);
    List<SolarSystem> findAllByType(SolarSystemType type);

    List<SolarSystem> findAllByTypeAndRelationOwnedBy(SolarSystemType solarSystemType, User user);
    List<SolarSystem> findAllByTypeAndRelationOwnedById(SolarSystemType solarSystemType, long user);
}
