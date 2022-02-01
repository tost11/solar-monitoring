package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolarService {
    @Autowired
    private InfluxConnection influxConnection;

    public void addSolarData(GenericInfluxPoint solarSystem, String token) {
        influxConnection.newPoint(solarSystem, token);
    }
}
