package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $id AND NOT s:NOT_FINISHED AND NOT s:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED"+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED"+
           "RETURN s,collect(ro), collect(ou) as relationOwnedBy, collect(rm), collect(mu) as relationManageBy")
    SolarSystem findByIdWithRelations(long id);

    @Query("MATCH (n:SolarSystem) "+
           "WHERE ID(n) = $id and not n:IS_DELETED and not n:NOT_FINISHED "+
           "RETURN n")
    SolarSystem findById(long id);

    @Query("MATCH (s:SolarSystem) <- [ro:owns] - (ou:User) "+
           "WHERE ID(s) = $systemId AND NOT s:NOT_FINISHED AND NOT s:IS_DELETED AND ID(ou) = $userId "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "RETURN s, collect(ro), collect(ou) as relationOwnedBy, collect(rm), collect(mu) as relationManageBy")
    List<SolarSystem> findAllByIdAndRelationOwnedByWithRelations(long systemId,long userId);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND ( rm.permission = \"ADMIN\" OR rm.permission = \"MANAGE\")) "+
           "RETURN s,collect(ro), collect(ou) as relationOwnedBy, collect(rm), collect(mu) as relationManageBy")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMangeWithRelations(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND rm.permission = \"ADMIN\") "+
           "RETURN s,collect(ro), collect(ou) as relationOwnedBy, collect(rm), collect(mu) as relationManageBy")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
        "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
        "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
        "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
        "WITH s,ro,ou,rm,mu "+
        "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND rm.permission = \"ADMIN\") "+
        "RETURN distinct s")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdmin(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND ( rm.permission = \"ADMIN\" OR rm.permission = \"MANAGE\")) "+
           "RETURN distinct s")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem and NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) AND NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) AND NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR ID(mu) = $idUser) "+
           "RETURN s")
    List<SolarSystem> findAllByIdAndRelationOwnsOrRelationManage(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem SET s:$label "+
           "RETURN s")
    SolarSystem addLabel(long system,String label);

    @Query("Match(s:SolarSystem) <- [r:owns] - (u:User) where ID(s) = $idSystem and not s:IS_DELETED and Not s:NOT_FINISHED and ID(u) = $idUser Return *")
    SolarSystem findByIdAndRelationOwnedById(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) <- [r:owns] - (u:User) "+
           "WHERE ID(u)  = $user and NOT s:IS_DELETED and NOT s:NOT_FINISHED AND s.type = $solarSystemType "+
           "RETURN *" +
           "ORDER BY s.creationDate ")
    List<SolarSystem> findAllByTypeAndRelationOwnedByIdWithOwnerRelation(SolarSystemType solarSystemType, long user);

    @Query("MATCH (s:SolarSystem) <- [r:owns] - (u:User) "+
           "WHERE ID(s) = $id AND NOT s:IS_DELETED and not s:NOT_FINISHED "+
           "RETURN *")
    SolarSystem findByIdWithOwner(long id);

    @Query("Match(n:SolarSystem) where ID(n) = $id and n:IS_DELETED Return n IS NOT Null")
    boolean existsByIdAndIsDeleted(long id);

    @Query("MATCH (s:SolarSystem) " +
           "WHERE s:NOT_FINISHED and s.creationDate < $date " +
           "RETURN s")
    List<SolarSystem> findAllNotInitializedAndCratedBefore(Instant date);

    @Query("Match(n:SolarSystem) " +
           "WHERE ID(n) = $id and not n:IS_DELETED AND NOT n:NOT_FINISHED " +
           "RETURN n")
    List<SolarSystem> findAllByType(SolarSystemType type);

    @Query("Match(u)-[r]->(s) where ID(u)= $userId and ID(s)= $systemId Delete r")
    void deleteRelationByUserIDAndSolarSystemID(long userId,long systemId);
}
