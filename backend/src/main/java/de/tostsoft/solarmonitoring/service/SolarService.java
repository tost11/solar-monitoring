package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.SecurityConfigurer;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolarService {

    @AllArgsConstructor
    @Getter
    private class CachedSystemInformation{
        private Date lastUpdated;
        private String userName;
        private String systemName;
    }

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void addSolarData(long systemId,GenericInfluxPoint genericInfluxPoint, String token) {

        SolarSystem system = solarSystemRepository.findById(systemId);
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user1 =system.getRelationOwnedBy();
        influxConnection.newPoint(system, genericInfluxPoint);
    }
}
