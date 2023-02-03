package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.service.InfluxTaskService;
import de.tostsoft.solarmonitoring.service.ManagerService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.StatusService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@Validated
@RequestMapping("/api/system")
public class SolarSystemController {

    @Autowired
    private SolarSystemService solarSystemService;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private ManagerService managerService;
    @Autowired
    private InfluxTaskService influxTaskService;
    @Autowired
    private StatusService statusService;

    private final Pattern namePattern = Pattern.compile("^[A-Za-z0-9_-äüöÄÜÖßé ]{3,30}$");

    public void validateAndFixSolarSystemDTO(RegisterSolarSystemDTO dto){
        Matcher m = namePattern.matcher(dto.getName());
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }
        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
    }

    public void validateAndFixSolarSystemDTO(PatchSolarSystemDTO dto){
        Matcher m = namePattern.matcher(dto.getName());
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }
        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
    }

    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody @Valid RegisterSolarSystemDTO registerSolarSystemDTO) {

        validateAndFixSolarSystemDTO(registerSolarSystemDTO);

        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/edit")
    public SolarSystemDTO patchSolarSystem(@RequestBody @Valid PatchSolarSystemDTO newSolarSystemDTO) {

        validateAndFixSolarSystemDTO(newSolarSystemDTO);

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(newSolarSystemDTO.getId(), user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        return solarSystemService.patchSolarSystem(newSolarSystemDTO, solarSystem);
    }

    @GetMapping("/{systemID}")
    public SolarSystemDTO getSystem(@PathVariable long systemID) {
        SolarSystemDTO returnDTO = solarSystemService.getSystemWithUserFromContextOrPublic(systemID);
        if(returnDTO == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }

        if(returnDTO.getType() == SolarSystemType.GRID){
            returnDTO.setHasACInput(false);
            returnDTO.setHasACOutput(true);
            returnDTO.setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.GRID_BATTERY){
            returnDTO.setHasACInput(true);
            returnDTO.setHasACOutput(true);
            returnDTO.setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.SIMPLE){
            returnDTO.setHasACInput(false);
            returnDTO.setHasACOutput(false);
            returnDTO.setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.VERY_SIMPLE){
            returnDTO.setHasACInput(false);
            returnDTO.setHasACOutput(false);
            returnDTO.setHasDCOutput(false);
        }

        return returnDTO;
    }

    @GetMapping("/all")
    public List<SolarSystemListItemDTO> getSystems() {
        return solarSystemService.getSystemsWithUserFromContext();
    }

    @GetMapping("/public/all")
    public List<SolarSystemListItemDTO> getSystemsPublic() {
        return solarSystemService.getPublicSystems();
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
        var solarSystem = solarSystemService.findSystemWithFullAccessWithAllRelations(systemId);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return managerService.getManagers(solarSystem);
    }

    @PostMapping("/deleteManager/{managerId}/{systemId}")
    public SolarSystemDTO deleteManager(@PathVariable long managerId, @PathVariable long systemId){
        var system = solarSystemService.findSystemWithFullAccessWithAllRelations(systemId);
        if(system == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no access on changing permissions on this system");
        }
         return managerService.deleteManager(system,managerId);
    }

    @GetMapping("/newToken/{id}")
    public NewTokenDTO newToken(@PathVariable long id) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return solarSystemService.createNewToken(solarSystem);
    }

    @GetMapping("/statistics/{id}")
    public void updateStatistics(@PathVariable long id){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SolarSystem solarSystem = solarSystemRepository.findWithOwnerByIdAndRelationOwnsOrRelationManageByAdminOrRelationManageByMange(id, user.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        if(!influxTaskService.runInitial(solarSystem)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN ,"This calculation is only allowed once a day try tomorrow");
        }
    }

    @PutMapping("/status/{id}")
    public BooleanStatusTDO setBooleanStatus(@PathVariable long id,@RequestParam String name){
        var solarSystem = solarSystemService.findSystemWithManageAccessWithOwner(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        return statusService.addStatus(name,false, solarSystem.getId(),solarSystem.getRelationOwnedBy().getId());
    }

    @DeleteMapping("/status/{id}")
    public void deleteBooleanStatus(@PathVariable long id,@RequestParam String name){
        var solarSystem = solarSystemService.findSystemWithManageAccessWithOwner(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        statusService.removeStatus(name, solarSystem.getId(),solarSystem.getRelationOwnedBy().getId());
    }

    @PostMapping("/status/{id}")
    public BooleanStatusTDO setBooleanStatus(@PathVariable long id,@RequestParam String name,@RequestParam Boolean value){
        var solarSystem = solarSystemService.findSystemWithManageAccessWithOwner(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        return statusService.setStatus(name,value, solarSystem.getId(),solarSystem.getRelationOwnedBy().getId());
    }

}