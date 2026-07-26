package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.app.monitoring.ApiMeterRegistry;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SolarService {

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private ApiMeterRegistry apiMeterRegistry;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public SolarSystem findMatchingSystemWithToken(String systemId, String token){
        var systemOpt = solarSystemRepository.findById(systemId);
        if(systemOpt.isEmpty()){
            //for upwards compatibility TODO remove later
            systemOpt = solarSystemRepository.findByInfluxTagName(systemId);
            if(systemOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: wrong credentials or system does not exist");
            }
        }
        var system = systemOpt.get();

        // Try new token list first
        if (!CollectionUtils.isEmpty(system.getTokens())) {
            for (AccessToken accessToken : system.getTokens()) {
                if (accessToken.getPurpose() != TokenPurpose.DATA_PUSH_REST) {
                    continue;
                }
                if (accessToken.getExpiresAt() != null && accessToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                    continue;
                }
                if (passwordEncoder.matches(token, accessToken.getHash())) {
                    return system;
                }
            }
        }

        // Fall back to legacy token field (backward compat during migration)
        if (system.getToken() != null && passwordEncoder.matches(token, system.getToken())) {
            return system;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: wrong credentials or system does not exist");
    }

    /**
     * Finds a system by ID (with influxTagName fallback). No token verification.
     * Throws 401 if not found.
     */
    public SolarSystem findSystemById(String systemId) {
        var systemOpt = solarSystemRepository.findById(systemId);
        if (systemOpt.isEmpty()) {
            systemOpt = solarSystemRepository.findByInfluxTagName(systemId);
            if (systemOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: wrong credentials or system does not exist");
            }
        }
        return systemOpt.get();
    }


    public SolarSystem findMatchingSystemWithDeyeSunSerial(Long serial){
        var systemOpt = solarSystemRepository.findSolarSystemBySerialInAndDeyeSunSerials(serial);
        if(systemOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return systemOpt.get();
    }

    public void addSolarData(SolarSystem solarSystem,GenericInfluxPoint genericInfluxPoint) {
        influxConnection.newPoint(solarSystem, genericInfluxPoint);
        apiMeterRegistry.incrementApiSamples(1);
    }

    public SolarInfluxPoint addSolarData(SolarSystem solarSystem, List<GenericInfluxPoint> genericInfluxPoint) {
        var last = influxConnection.newPoints(solarSystem, genericInfluxPoint);
        apiMeterRegistry.incrementApiSamples(genericInfluxPoint.size());
        return last;
    }
}
