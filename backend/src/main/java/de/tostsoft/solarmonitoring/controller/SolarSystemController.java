package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.model.Permissions;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import java.util.List;
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


    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody RegisterSolarSystemDTO registerSolarSystemDTO) {
        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/patch")
    public SolarSystemDTO patchSolarSystem(@RequestBody SolarSystemDTO newSolarSystemDTO) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnsAndRelationManageByAdminOrManage(newSolarSystemDTO.getId(), user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        return solarSystemService.patchSolarSystem(newSolarSystemDTO,solarSystem);
    }

    @GetMapping("/{systemID}")
    public SolarSystemDTO getSystem(@PathVariable long systemID) {
        SolarSystemDTO returnDTO = solarSystemService.getSystemWithUserFromContext(systemID);
        if(returnDTO == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"You have no access on this System");
        return returnDTO;
    }

    @GetMapping("/all")
    public List<SolarSystemListItemDTO> getSystems() {
        return solarSystemService.getSystems();
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> deleteSystem(@PathVariable long id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnedById(id, user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return solarSystemService.deleteSystem(solarSystem);
    }
    @PostMapping("/addManageBy/{id}/{solarID}/{permission}")
    public SolarSystemDTO setMangeUser (@PathVariable long id,@PathVariable long solarID,@PathVariable Permissions permission) {
        return solarSystemService.addManageUser(id,solarID,permission);
    }
    @GetMapping("/allManager/{systemId}")
    public List<ManagerDTO> getManagers(@PathVariable long systemId){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<SolarSystem> solarSystem = solarSystemRepository.findAllByIdAndRelationOwnedByAndLoadManager(systemId,user.getId());
        if(solarSystem == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Its nor your system");

        return solarSystemService.getManagers(solarSystem.get(0));
    }


}
