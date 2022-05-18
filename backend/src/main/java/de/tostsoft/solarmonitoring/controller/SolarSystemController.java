package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.NewTokenDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.service.ManagerService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import java.util.List;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private ManagerService managerService;

    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody RegisterSolarSystemDTO registerSolarSystemDTO) {
        TimeZone.getTimeZone(registerSolarSystemDTO.getTimezone());
        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/edit")
    public SolarSystemDTO patchSolarSystem(@RequestBody SolarSystemDTO newSolarSystemDTO) {
        TimeZone.getTimeZone(newSolarSystemDTO.getTimezone());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(newSolarSystemDTO.getId(), user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        if((InfluxController.GRID_SYSTEM_TYPES.contains(newSolarSystemDTO.getType().toString()) && !InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString())) ||
            (InfluxController.SIMPLE_SYSTEM_TYPES.contains(newSolarSystemDTO.getType().toString()) && !InfluxController.SIMPLE_SYSTEM_TYPES.contains(solarSystem.getType().toString()))  ||
            (InfluxController.GRID_SYSTEM_TYPES.contains(newSolarSystemDTO.getType().toString()) && !InfluxController.GRID_SYSTEM_TYPES.contains(solarSystem.getType().toString()))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This type conversion is not allowed");
        }
        return solarSystemService.patchSolarSystem(newSolarSystemDTO, solarSystem);
    }

    @GetMapping("/{systemID}")
    public SolarSystemDTO getSystem(@PathVariable long systemID) {
        SolarSystemDTO returnDTO = solarSystemService.getSystemWithUserFromContext(systemID);
        if(returnDTO == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }
        return returnDTO;
    }

    @GetMapping("/all")
    public List<SolarSystemListItemDTO> getSystems() {
        return solarSystemService.getSystemsWithUserFromContext();
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteSystem(@PathVariable long id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnedById(id, user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return solarSystemService.deleteSystem(solarSystem);
    }

    @PostMapping( "/addManageBy")
    public SolarSystemDTO setMangeUser (@RequestBody AddManagerDTO addManagerDTO) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var system = solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminWithRelations(addManagerDTO.getSystemId(),user.getId());
        if(system == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no access on changing permissions on this system");
        }
        if(system.getRelationOwnedBy().getId() == addManagerDTO.getId()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cann not add yourself as manager");
        }
        return managerService.addManageUser(system,addManagerDTO);
    }

    //TODO make use of system functions
    @GetMapping("/allManager/{systemId}")
    public List<ManagerDTO> getManagers(@PathVariable long systemId) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId,true);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return managerService.getManagers(solarSystem);
    }
    @PostMapping("/deleteManager/{managerId}/{systemId}")
    private SolarSystemDTO deleteManager(@PathVariable long managerId, @PathVariable long systemId){
        var system = solarSystemService.findSystemWithFullAccess(systemId,true);
        if(system == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no access on changing permissions on this system");
        }
         return managerService.deleteManager(system,managerId);
    }


    @GetMapping("/newToken/{id}")
    public NewTokenDTO newToken(@PathVariable long id) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(id,false);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return solarSystemService.createNewToken(solarSystem);
    }
}