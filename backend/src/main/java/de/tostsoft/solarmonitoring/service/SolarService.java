package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
<<<<<<< HEAD

        SolarSystem system = solarSystemRepository.findById(systemId);
=======
        var system = solarSystemRepository.findByIdAndLoadOwner(systemId);
>>>>>>> 5b1489e4d96ae9fc0a96e7faf01b704c9000d155
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        influxConnection.newPoint(system, genericInfluxPoint);
    }
}
