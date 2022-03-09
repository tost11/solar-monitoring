package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
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
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public SolarSystem findMatchingSystemWithToken(long systemId, String token){
        var system = solarSystemRepository.findByIdWithOwner(systemId);
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return system;
    }

    public void addSolarData(long systemId,GenericInfluxPoint genericInfluxPoint, String token) {
        var system = solarSystemRepository.findByIdWithOwner(systemId);

        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        influxConnection.newPoint(system, genericInfluxPoint);
    }

    public void addSolarData(SolarSystem solarSystem,GenericInfluxPoint genericInfluxPoint) {
        influxConnection.newPoint(solarSystem, genericInfluxPoint);
    }
}
