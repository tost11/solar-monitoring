package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.GettingSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.service.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SolarSystemController {
    @Autowired
    private SolarSystemService solarSystemService;


    @PostMapping
    public SolarSystemDTO newSolar(@RequestBody RegisterSolarSystemDTO registerSolarSystemDTO)  {

        return solarSystemService.add(registerSolarSystemDTO);
    }

    @GetMapping("/{systemID}")
    public GettingSolarSystemDTO getSystem(@PathVariable long systemID) {

        return solarSystemService.getSystem(systemID);
    }

    @GetMapping("/all")
    public List<GettingSolarSystemDTO> getSystems() {

        return solarSystemService.getSystems();
    }

    @DeleteMapping("/{token}")
    public void deleteSystem(@PathVariable String token) throws Exception {
        solarSystemService.deleteSystem(token);
    }

}
