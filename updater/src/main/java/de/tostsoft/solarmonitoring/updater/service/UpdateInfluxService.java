package de.tostsoft.solarmonitoring.updater.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class UpdateInfluxService {

    @Autowired
    private InfluxConnection influxConnection;

    /*public boolean isOnlineAround(SolarSystem solarSystem, LocalDateTime localDateTime, Duration duration){

    }*/
}
