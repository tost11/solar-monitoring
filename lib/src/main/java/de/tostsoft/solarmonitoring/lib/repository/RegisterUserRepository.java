package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.RegisterUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegisterUserRepository extends MongoRepository<RegisterUser,String> {

    long countByName(String name);

    long countByMail(String mail);

    RegisterUser findByName(String name);

    void deleteAllByCreatedAtBefore(long epochMilli);
}
