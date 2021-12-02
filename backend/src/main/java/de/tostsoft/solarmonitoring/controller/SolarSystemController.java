package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SolarSystemController {
    @Autowired
    private SolarSystemService solarSystemService;

    @PostMapping
    public SolarSystem newSolar (@RequestBody SolarSystemDTO solarSystemDTO){
        return solarSystemService.add(solarSystemDTO);
    }

    @GetMapping("/{token}")
    public SolarSystem getSystem(@PathVariable String token){

        return solarSystemService.getSystem(token);
    }

    @DeleteMapping("/{token}")
    public void deleteSystem(@PathVariable String token) throws Exception {
        solarSystemService.deleteSystem(token);
    }

}
