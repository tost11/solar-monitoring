package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import jdk.jfr.Label;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import de.tostsoft.solarmonitoring.model.User;
import java.util.List;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {
    Boolean existsAllByToken(String token);
    @Query("Match(n:SolarSystem) where ID(n) = $id and not n:IS_DELETED and not n:NOT_FINISHED Return n")
    SolarSystem findById(long id);
    @Query("Match(n:SolarSystem) <- [:owns] - (u:User) where ID(u) = $id and not n:IS_DELETED and n:NOT_FINISHED Return n")
    List<SolarSystem> findAllByRelationOwnedById(long id);




    SolarSystem findSolarSystemByIdAndLabelsNotContainsOrLabelsNotContains(long id,String label,String label2);
    boolean existsByLabelsContains(String label);

    boolean existsByName(String name);

    List<SolarSystem> findAllByType(SolarSystemType type);

    List<SolarSystem> findAllByTypeAndRelationOwnedBy(SolarSystemType solarSystemType, User user);
    List<SolarSystem> findAllByTypeAndRelationOwnedById(SolarSystemType solarSystemType, long user);
}
