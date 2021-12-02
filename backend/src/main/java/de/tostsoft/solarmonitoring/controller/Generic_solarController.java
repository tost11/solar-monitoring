package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.Connection;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarSystem;
import de.tostsoft.solarmonitoring.model.SelfMadeWithInverterSolarSystem;
import de.tostsoft.solarmonitoring.service.Generic_solarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.crypto.Data;
import java.util.Date;

@RestController
@RequestMapping("/api/solar")
public class Generic_solarController {

    @Autowired
    Generic_solarService generic_solarService;
    @Autowired
    Connection connection;

    @PostMapping("/Test")
    public GenericInfluxPoint PostTestSolar() {
        return generic_solarService.addTestSolar(0);
    }
    @PostMapping("/data/SelfMadeWithInverter")
    public void PostData(@RequestBody SelfMadeWithInverterSolarSystem solarSystem) throws Exception {

        generic_solarService.addSolarData(solarSystem,null);
    }
    @PostMapping("/data/SelfMade")
    public void PostData(@RequestBody SelfMadeSolarSystem solarSystem, @RequestHeader String clientToken ) throws Exception {
        solarSystem.setMeasurement(GenericInfluxPoint.InfliuxSolarMeasurement.SELFMADE);
        if(solarSystem.getTimeStep() == null || solarSystem.getTimeStep() <= 0){
            solarSystem.setTimeStep(new Date().getTime());
        }
        generic_solarService.addSolarData(solarSystem,clientToken);
    }


}

