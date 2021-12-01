package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.module.User;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    User findByNameIgnoreCase(String name);
    int countByNameIgnoreCase(String name);





}
