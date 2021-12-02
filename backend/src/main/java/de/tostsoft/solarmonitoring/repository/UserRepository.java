package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    User findByNameIgnoreCase(String name);
    int countByNameIgnoreCase(String name);





}
