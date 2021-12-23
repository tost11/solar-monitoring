package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SolarSystemController {
    @Autowired
    private SolarSystemService solarSystemService;

    @PostMapping
    public SolarSystemDTO newSolar(@RequestBody SolarSystemDTO solarSystemDTO) {
        return solarSystemService.add(solarSystemDTO);
    }

    @GetMapping("/{SystemID}")
    public SolarSystem getSystem(@PathVariable String token) {

        return solarSystemService.getSystem(token);
    }

    @GetMapping("/all")
    public List<SolarSystemDTO> getSystems() {

        return solarSystemService.getSystems();
    }

    @DeleteMapping("/{token}")
    public void deleteSystem(@PathVariable String token) throws Exception {
        solarSystemService.deleteSystem(token);
    }

}
