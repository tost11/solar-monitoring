package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.app.monitoring.ApiMeterRegistry;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System with this id dose not exist");
            }
        }
        var system = systemOpt.get();
        if(!passwordEncoder.matches(token,system.getToken())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return system;
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
