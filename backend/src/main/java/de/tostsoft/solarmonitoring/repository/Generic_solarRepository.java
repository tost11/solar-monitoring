package de.tostsoft.solarmonitoring.repository;

import org.springframework.boot.autoconfigure.influx.InfluxDbCustomizer;
import org.springframework.stereotype.Repository;

@Repository
public interface Generic_solarRepository extends InfluxDbCustomizer {

}
