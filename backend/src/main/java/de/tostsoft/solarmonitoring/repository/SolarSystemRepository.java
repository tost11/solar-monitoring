package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends Neo4jRepository<SolarSystem, Long> {

    String fetchDataReturnPart =
            "WITH s,ro,ou,rm,mu " +
            "RETURN distinct s,collect(ro), collect(ou) as relationOwnedBy, collect(rm), collect(mu) as relationManageBy";

    String fetchDataQueryPart =
            "OPTIONAL MATCH (s) <- [__ro:owns] - (__ou:User) WHERE NOT ou:IS_DELETED " +
            "OPTIONAL MATCH (s) <- [__rm:manages] - (__mu:User) WHERE NOT mu:IS_DELETED " +
            "WITH s,__ro,__ou,__rm,__mu " +
            "RETURN distinct s,collect(__ro), collect(__ou) as relationOwnedBy, collect(__rm), collect(__mu) as relationManageBy";

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $id AND NOT s:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED"+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED"+
            fetchDataQueryPart)
    SolarSystem findByIdWithRelations(long id);

    @Query("MATCH (n:SolarSystem) "+
           "WHERE ID(n) = $id and not n:IS_DELETED "+
           "RETURN n")
    SolarSystem findById(long id);

    @Query("MATCH (s:SolarSystem) "+
            "WHERE ID(s) = $systemId AND NOT s:IS_DELETED "+
            "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
            "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
            "WITH s,ro,ou,rm,mu "+
            "WHERE ID(ou) = $userId OR ID(mu) = $userId "+
            fetchDataQueryPart)
    SolarSystem findByIdAndRelationOwnedOrRelationManageWithRelations(long systemId,long userId);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND ( rm.permission = \"ADMIN\" OR rm.permission = \"MANAGE\")) "+
            fetchDataQueryPart)
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMangeWithRelations(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) WHERE ID(s)=$idSystem AND NOT s:IS_DELETED " +
            "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED " +
            "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED " +
            "WITH s,ro,ou,rm,mu " +
            "WHERE ID(ou)=$idUser OR (ID(mu) = $idUser AND rm.permission = \"ADMIN\") "+
            fetchDataQueryPart)
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
        "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED "+
        "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
        "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
        "WITH s,ro,ou,rm,mu "+
        "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND rm.permission = \"ADMIN\") "+
        "RETURN distinct s")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdmin(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem AND NOT s:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) WHERE NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) WHERE NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR (ID(mu) = $idUser AND ( rm.permission = \"ADMIN\" OR rm.permission = \"MANAGE\")) "+
           "RETURN distinct s")
    SolarSystem findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem ç "+
           "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) where  NOT ou:IS_DELETED "+
           "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) where  NOT mu:IS_DELETED "+
           "WITH s,ro,ou,rm,mu "+
           "WHERE ID(ou) = $idUser OR ID(mu) = $idUser "+
            fetchDataQueryPart)
    SolarSystem findByIdAndRelationOwnsOrRelationManageWithRelations(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) "+
        "WHERE ID(s) = $idSystem and NOT s:IS_DELETED AND NOT s:NOT_FINISHED "+
        "OPTIONAL MATCH (s) <- [ro:owns] - (ou:User) where  NOT ou:IS_DELETED "+
        "OPTIONAL MATCH (s) <- [rm:manages] - (mu:User) where  NOT mu:IS_DELETED "+
        "WITH s,ro,ou,rm,mu "+
        "WHERE ID(ou) = $idUser OR ID(mu) = $idUser "+
        "RETURN s")
    SolarSystem findByIdAndRelationOwnsOrRelationManage(long idSystem,long idUser);



    @Query("MATCH (s:SolarSystem) "+
           "WHERE ID(s) = $idSystem SET s:$label "+
           "RETURN s")
    SolarSystem addLabel(long idSystem,String label);

    @Query("MATCH (s:SolarSystem) "+
            "WHERE ID(s) = $idSystem SET s:IS_DELETED "+
            "RETURN s")
    SolarSystem addDeleteLabel(long idSystem);

    @Query("Match(s:SolarSystem) <- [r:owns] - (u:User) where ID(s) = $idSystem and not s:IS_DELETED and ID(u) = $idUser Return *")
    SolarSystem findByIdAndRelationOwnedById(long idSystem,long idUser);

    @Query("MATCH (s:SolarSystem) <- [r:owns] - (u:User) "+
           "WHERE ID(u)  = $user and NOT s:IS_DELETED  AND s.type = $solarSystemType "+
           "RETURN *" +
           "ORDER BY s.creationDate ")
    List<SolarSystem> findAllByTypeAndRelationOwnedByIdWithOwnerRelation(SolarSystemType solarSystemType, long user);

    @Query("MATCH (s:SolarSystem) <- [r:owns] - (u:User) "+
           "WHERE ID(s) = $id AND NOT s:IS_DELETED "+
           "RETURN *")
    SolarSystem findByIdWithOwner(long id);

    @Query("Match(n:SolarSystem) where ID(n) = $id and n:IS_DELETED Return n IS NOT Null")
    boolean existsByIdAndIsDeleted(long id);

    @Query("Match(n:SolarSystem) " +
           "WHERE ID(n) = $id and not n:IS_DELETED " +
           "RETURN n")
    List<SolarSystem> findAllByType(SolarSystemType type);

    @Query("Match(u:User)-[r]->(s:SolarSystem) where ID(r) = $relationId SET r.permission = $permission")
    void updateManageRelation(long relationId,String permission);

    @Query("Match(u:User), (s:SolarSystem) where ID(u) = $userId and ID(s) = $systemId CREATE (u) - [r:manages{permission:$permission}] -> (s) return ID(r)")
    Long addManageRelation(long userId,long systemId,String permission);

    @Query("Match(u:User)-[r]->(s:SolarSystem) WHERE ID(r) = $relationId DELETE r")
    Long deleteManagerRelation(long relationId);
}
