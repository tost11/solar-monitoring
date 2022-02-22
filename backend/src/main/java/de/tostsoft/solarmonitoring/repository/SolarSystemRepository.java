package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.model.User;

import java.time.Instant;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {

    @Query("Match(n:SolarSystem) - [r] - (u) where ID(n) = $id and not n:IS_DELETED and not n:NOT_FINISHED Return n,r,u")
    SolarSystem findById(long id);


    @Query("Match(n:SolarSystem) <- [r:owns] - (u:User) where ID(n) = $id and not n:IS_DELETED and not n:NOT_FINISHED Return n,r,u")
    SolarSystem findByIdAndLoadOwner(long id);

    @Query("Match(n:SolarSystem) <- [r:owns] - (u:User) where ID(u) = $idSystem and not n:IS_DELETED and Not n:NOT_FINISHED and ID(n) = $idUser Return n,r,u")
    SolarSystem findByIdAndRelationOwnedById(long idSystem,long idUser);

    //@Query("Match(s:SolarSystem) <- [r:owns] - (u:User) where ID(u) = $userId and not s:IS_DELETED and Not s:NOT_FINISHED Return s {identity:ID(s),labels:[labels(s)],properties:{type:s.type,creationDate:s.creationDate,name:s.name}} ORDER BY s.creationDate")

    @Query("Match(s:SolarSystem) <- [r:owns] - (u:User) where ID(u) = $userId and not s:IS_DELETED and Not s:NOT_FINISHED Return s ORDER BY s.creationDate")
    List<SolarSystem> findAllByOwnerWithBasicInformation(long userId);

    @Query("Match(n:SolarSystem) <- [:owns] - (u:User) where ID(u) = $id and not n:IS_DELETED and Not n:NOT_FINISHED Return n")
    List<SolarSystem> findAllByRelationOwnedById(long id);

    @Query("Match(n:SolarSystem) where ID(n) = $id and n:IS_DELETED Return n IS NOT Null")
    boolean existsByIdAndIsDeleted(long id);

    @Query("Match(s:SolarSystem) WHERE s:NOT_FINISHED and s.creationDate < $date  Return s")
    List<SolarSystem> findAllNotInitializedAndCratedBefore(Instant date);

    List<SolarSystem> findAllByType(SolarSystemType type);

    List<SolarSystem> findAllById(long id);

    List<SolarSystem> findAllByTypeAndRelationOwnedBy(SolarSystemType solarSystemType, User user);
    List<SolarSystem> findAllByTypeAndRelationOwnedById(SolarSystemType solarSystemType, long user);
}
