package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.module.SolarSystem;
import org.springframework.boot.autoconfigure.influx.InfluxDbCustomizer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolarSystemRepository extends CrudRepository<SolarSystem,Long> {
        SolarSystem existsAllByToken(String token);
}
