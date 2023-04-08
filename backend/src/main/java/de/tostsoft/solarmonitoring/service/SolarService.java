package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolarService {

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private Neo4jSolarSystemRepository neo4jSolarSystemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Neo4jSolarSystem findMatchingSystemWithToken(long systemId, String token){
        var system = neo4jSolarSystemRepository.findByIdWithOwner(systemId);
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return system;
    }

    public void addSolarData(Neo4jSolarSystem neo4jSolarSystem,GenericInfluxPoint genericInfluxPoint) {
        influxConnection.newPoint(neo4jSolarSystem, genericInfluxPoint);
    }

    public void addSolarData(Neo4jSolarSystem neo4jSolarSystem, List<GenericInfluxPoint> genericInfluxPoint) {
        influxConnection.newPoints(neo4jSolarSystem, genericInfluxPoint);
    }
}
