package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.Connection;
import de.tostsoft.solarmonitoring.service.Generic_solarService;
import de.tostsoft.solarmonitoring.module.Generic_solar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/solar")
public class Generic_solarController {

    @Autowired
    Generic_solarService generic_solarService;
    @Autowired
    Connection connection;

@PostMapping
    public Generic_solar PostTestSolar (){
        return generic_solarService.addTestSolar(0);
    }
}
