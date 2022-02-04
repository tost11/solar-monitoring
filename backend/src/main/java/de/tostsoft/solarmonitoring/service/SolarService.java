package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.Date;
import java.util.HashMap;
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
    private PasswordEncoder passwordEncoder;

    HashMap<String,CachedSystemInformation> cachedSystems = new HashMap<>();

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    public void addSolarData(long systemId,GenericInfluxPoint genericInfluxPoint, String token) {

        //validation user is permitted to to that

        var system = solarSystemRepository.findSolarSystemByIdAndLabelsNotContainsOrLabelsNotContains(systemId, Neo4jLabels.NOT_FINISHED.toString(),Neo4jLabels.IS_DELETED.toString());
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        influxConnection.newPoint(system, genericInfluxPoint);
    }
}
