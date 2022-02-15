package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/system")
public class SolarSystemController {
    @Autowired
    private SolarSystemService solarSystemService;


    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody RegisterSolarSystemDTO registerSolarSystemDTO)  {
        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/patch")
    public SolarSystemDTO patchSolarSystem(@RequestBody SolarSystemDTO newSolarSystemDTO){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        for(SolarSystem ownSystems:user.getRelationOwns()){
            if(ownSystems.getId().equals(newSolarSystemDTO.getId())){
                return solarSystemService.patchSolarSystem(newSolarSystemDTO);
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"This is not your system");


    }
    @GetMapping("/{systemID}")
    public SolarSystemDTO getSystem(@PathVariable long systemID) {

        return solarSystemService.getSystem(systemID);
    }

    @GetMapping("/all")
    public List<SolarSystemListItemDTO> getSystems() {

        return solarSystemService.getSystems();
    }

    @PostMapping("/{id}")
    public void deleteSystem(@PathVariable long id){
        solarSystemService.deleteSystem(id);
    }

}
