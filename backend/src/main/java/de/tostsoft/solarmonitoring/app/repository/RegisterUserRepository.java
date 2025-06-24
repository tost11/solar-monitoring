package de.tostsoft.solarmonitoring.app.repository;

import de.tostsoft.solarmonitoring.app.model.RegisterUser;
import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegisterUserRepository extends MongoRepository<RegisterUser,String> {

    long countByName(String name);

    long countByMail(String mail);

    RegisterUser findByName(String name);
}
