package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.module.SolarSystem;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/system")
public class SolarSystemController {
    @Autowired
    private SolarSystemService solarSystemService;

    @PostMapping
    public SolarSystem newSolar (@RequestBody SolarSystemDTO solarSystemDTO){
        return solarSystemService.add(solarSystemDTO);
    }

}
