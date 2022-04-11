package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {

    @Query("MATCH (u:User) " +
           "WHERE toLower(u.name) = toLower($name) AND not u:IS_DELETED " +
           "RETURN u")
    User findByNameIgnoreCase(String name);

    @Query("MATCH (u:User) " +
           "WHERE ID(u)=$id AND not u:IS_DELETED " +
           "RETURN u")
    User findById(long id);

    int countByNameIgnoreCase(String name);

    @Query("MATCH (u:User) " +
           "WHERE ID(u) = $id AND not u:IS_DELETED " +
           "OPTIONAL MATCH (u) - [ro:owns] -> (so) " +
           "OPTIONAL MATCH (u) - [rm:manages] -> (sm) " +
           "WITH u,ro,so,rm,sm " +
           "RETURN u,collect(ro), collect(so) as relationOwns, collect(rm), collect(sm) as relationManageBy")
    User findByIdAndLoadRelations(long id);

    @Query("MATCH (u:User) "+
          "WHERE ID(u) = $UserId AND not u:IS_DELETED "+
          "OPTIONAL MATCH (u) - [ro:owns] -> (so) Where not so:IS_DELETED "+
          "OPTIONAL MATCH (u) - [rm:manages] -> (sm) Where not sm:IS_DELETED "+
          "WITH u,ro,so,rm,sm "+
          "RETURN u,collect(ro), collect(so) as relationOwns, collect(rm), collect(sm) as relationManageBy")
    User findByIdAndLoadRelationsNotDeleted(long UserId);

    @Query("MATCH (n)-[r]->(s) where ID(n)=$id and not s:IS_DELETED RETURN COUNT(r)")
    int countByRelationOwns(long id);

    @Query("MATCH (u:User) - [:owns|:manages] -> (s:SolarSystem) WHERE ID(s) = $systemId AND ID(u) = $userId AND NOT s:IS_DELETED MATCH (s) <- [:owns] - (ou:User) return ID(ou)")
    long findOwnerIDByUserIDOrManagerID(long systemId,long userId);

    @Query("MATCH (u:User) - [:owns|:manages] -> (s:SolarSystem) WHERE ID(s) = $systemId AND s.type in $types AND ID(u) = $userId AND NOT s:IS_DELETED MATCH (s) <- [:owns] - (ou:User) return ID(ou)")
    long findOwnerIDByUserIDOrManagerIDAdSystemTypeIn(long systemId,long userId,List<String> types);

    @Query("CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (user:User) ASSERT user.name IS UNIQUE")
    void initNameConstrain();

    @Query("Match(u:User) WHERE toLower(u.name) STARTS WITH toLower($name) Return u LIMIT 10")
    List<User> findAllInitializedAndAdminStartsWith(String name);

    @Query("MATCH(u:User) WHERE ID(u) = $userId RETURN u.isAdmin")
    boolean isUserAdmin(long userId);
}
