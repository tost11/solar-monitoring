package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import de.tostsoft.solarmonitoring.model.User;
import java.util.List;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {

    @Query("Match(n:SolarSystem) - [r] - (u) where ID(n) = $id and not n:IS_DELETED and not n:NOT_FINISHED Return n,r,u")
    SolarSystem findById(long id);


    SolarSystem findByIdAndRelationOwnedById(long idSystem,long idUser);

    @Query("Match(n:SolarSystem) <- [:owns] - (u:User) where ID(u) = $id and not n:IS_DELETED and Not n:NOT_FINISHED Return n")
    List<SolarSystem> findAllByRelationOwnedById(long id);

    @Query("Match(n:SolarSystem) where ID(n) = $id and n:IS_DELETED Return n IS NOT Null")
    boolean existsByIdAndIsDeleted(long id);


    List<SolarSystem> findAllByType(SolarSystemType type);

    List<SolarSystem> findAllById(long id);

    List<SolarSystem> findAllByTypeAndRelationOwnedBy(SolarSystemType solarSystemType, User user);
    List<SolarSystem> findAllByTypeAndRelationOwnedById(SolarSystemType solarSystemType, long user);
}
