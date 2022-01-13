package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.SolarSystem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends CrudRepository<SolarSystem, Long> {
    SolarSystem existsAllByToken(String token);

    SolarSystem findByToken(String token);

    SolarSystem findById(long id);

    void deleteByToken(String token);

    boolean existsByName(String name);
}
