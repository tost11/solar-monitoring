package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByNameIgnoreCase(String name);

    Optional<User> findBy(String name);

    int countByNameIgnoreCase(String name);


}
