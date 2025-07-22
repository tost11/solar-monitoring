package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.RegisterUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.validation.annotation.Validated;

@Validated
public interface RegisterUserRepository extends MongoRepository<RegisterUser,String> {

    long countByName(@NotNull String name);

    long countByMail(@NotNull String mail);

    RegisterUser findByName(@NotNull String name);

    void deleteAllByCreatedAtBefore(long epochMilli);

    RegisterUser findOneByNameOrMail(@NotNull String name,@NotNull String mail);
}
