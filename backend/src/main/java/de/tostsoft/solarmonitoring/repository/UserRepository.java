package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import java.util.Optional;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByNameIgnoreCase(String name);

    Optional<User> findBy(String name);

    int countByNameIgnoreCase(String name);

    @Query("MATCH (u:User) - [owns] -> (s:SolarSystem{token:$token}) return ID(u)")
    Long findUserIdBySystemToken(String token);

    @Query("MATCH (u:User) - [owns] -> (s:SolarSystem{token:$token}) return u.name")
    String findUsernameBySystemToken(String token);
}
