package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import retrofit2.http.QueryMap;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {



    @Query("MATCH (u:User) WHERE toLower(u.name) = toLower($name) return u")
    User findByNameIgnoreCase(String name);

    @Query("Match(u:User) WHERE u.name STARTS WITH $name and NOT u:NOT_FINISHED Return u LIMIT 10")
    List<User> findAllInitializedAndAdminStartsWith(String name);

   // @Query("MATCH (u:User) WHERE ID(u)=$id return u")
    User findUserById(long id);

    int countByNameIgnoreCase(String name);

    @Query("Match(u:User) WHERE ID(u)=$id set u=$user")
    User UpdateUserWithoutRelations(long id,@QueryMap Map<String,String> user );

    @Query("Match(u:User) WHERE ID(u) = $id AND NOT u:NOT_FINISHED and not u:IS_DELETED OPTIONAL MATCH (u) - [ro:owns] -> (so) OPTIONAL MATCH (u) - [rm:manages] -> (sm) return u,collect(ro), collect(so) as relationOwns, collect(rm), collect(sm) as relationManageBy")
    User findByIdAndLoadRelations(long id);

    @Query("Match(u:User) WHERE u:NOT_FINISHED and u.creationDate < $date  Return u")
    List<User> findAllNotInitializedAndCratedBefore(Instant date);

    @Query("MATCH (n)-[r]->() where ID(n)=$id RETURN COUNT(r)")
    int countByRelationOwns(long id);

    @Query("MATCH (u:User) - [owns] -> (s:SolarSystem{token:$token}) return ID(u)")
    Long findUserIdBySystemToken(String token);

    @Query("CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (user:User) ASSERT user.name IS UNIQUE")
    void initNameConstrain();

    User findAllByNameLike(String userName);


}
