package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.Converter;
import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.InfluxTaskService;
import de.tostsoft.solarmonitoring.service.ManagerService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.StatusService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
    private ManagerService managerService;

    @Autowired
    private InfluxTaskService influxTaskService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserRepository userRepository;

    private final Pattern namePattern = Pattern.compile("^[A-Za-z0-9_\\-äüöÄÜÖßé ]{3,30}$");
    private final Pattern numberPattern = Pattern.compile("^[1-9][0-9]*$");

    public void validateAndFixSolarSystemDTO(RegisterSolarSystemDTO dto){
        var trimmedName = StringUtils.trim(dto.getName());
        Matcher m = namePattern.matcher(trimmedName);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }
        dto.setName(trimmedName);
        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
        validateNamings(dto.getNamings());
    }

    public String validateName(String name){
        var trimmedName = StringUtils.trim(name);
        Matcher m = namePattern.matcher(trimmedName);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }
        return trimmedName;
    }

    public String validateNaming(String name){
        var trimmedName = StringUtils.trim(name);
        Matcher m = namePattern.matcher(trimmedName);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Naming dose not match requirements");
        }
        return trimmedName;
    }

    public void validateDeviceId(String name){
        Matcher m = numberPattern.matcher(name);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Naming ID dose not match requirements");
        }
    }

    public void validateInputOutputOrBatteryId(String name){
        var arr = StringUtils.split(name,"-");
        if(arr.length != 2){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Naming ID dose not match requirements");
        }
        for (String s : arr) {
            Matcher m = numberPattern.matcher(s);
            if(!m.matches()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Naming ID dose not match requirements");
            }
        }
    }

    public void validateNamings(NamingsDTO namingsDTO){
        namingsDTO.getDevices().entrySet().forEach(e->{
            validateDeviceId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });

        namingsDTO.getInputsDC().entrySet().forEach(e->{
            validateInputOutputOrBatteryId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });

        namingsDTO.getInputsAC().entrySet().forEach(e->{
            validateInputOutputOrBatteryId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });

        namingsDTO.getOutputsDC().entrySet().forEach(e->{
            validateInputOutputOrBatteryId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });

        namingsDTO.getOutputsAC().entrySet().forEach(e->{
            validateInputOutputOrBatteryId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });

        namingsDTO.getBatteries().entrySet().forEach(e->{
            validateInputOutputOrBatteryId(e.getKey());
            e.setValue(validateNaming(e.getValue()));
        });
    }

    public void validateAndFixSolarSystemDTO(PatchSolarSystemDTO dto){
        validateName(dto.getName());
        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
        validateNamings(dto.getNamings());
    }

    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody @Valid RegisterSolarSystemDTO registerSolarSystemDTO) {

        validateAndFixSolarSystemDTO(registerSolarSystemDTO);

        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/edit")
    public SolarSystemDTO patchSolarSystem(@RequestBody @Valid PatchSolarSystemDTO newSolarSystemDTO) {

        validateAndFixSolarSystemDTO(newSolarSystemDTO);

        var solarSystem = solarSystemService.findSystemWithMangeAccess(newSolarSystemDTO.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        return solarSystemService.patchSolarSystem(newSolarSystemDTO, solarSystem);
    }

    @GetMapping("/{systemID}")
    public SolarSystemDTO getSystem(@PathVariable String systemID) {
        SolarSystemDTO returnDTO = solarSystemService.getSystemWithUserFromContextOrPublic(systemID);
        if(returnDTO == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }

        if(returnDTO.getType() == SolarSystemType.GRID){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(true);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.GRID_BATTERY){
            returnDTO.getViewData().setHasACInput(true);
            returnDTO.getViewData().setHasACOutput(true);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.SIMPLE){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(false);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(returnDTO.getType() == SolarSystemType.VERY_SIMPLE){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(false);
            returnDTO.getViewData().setHasDCOutput(false);
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
    public ResponseEntity<String> deleteSystem(@PathVariable String id) {
        var solarSystem = solarSystemService.findSystemWithOwnedBy(id);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return solarSystemService.deleteSystem(solarSystem);
    }

    @PostMapping( "/addManageBy")
    public List<ManagerDTO> setMangeUser (@RequestBody AddManagerDTO addManagerDTO) {
        var system = solarSystemService.findSystemWithFullAccess(addManagerDTO.getSystemId());
        if(system == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"You have no access on changing permissions on this system");
        }
        var managerOpt = userRepository.findById(addManagerDTO.getId());
        if(managerOpt.isEmpty()){
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if(system.getOwnedBy().equals(managerOpt.get())){
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cann not add yourself as manager");
        }
        return Converter.convertListManagesToManagerDTO(managerService.addOrUpdateManageUser(system,addManagerDTO,managerOpt.get()));
    }

    @GetMapping("/allManager/{systemId}")
    public List<ManagerDTO> getManagers(@PathVariable String systemId) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return Converter.convertListManagesToManagerDTO(solarSystem.getManagedBy());
    }

    @PostMapping("/deleteManager/{managerId}/{systemId}")
    public  List<ManagerDTO> deleteManager(@PathVariable String managerId, @PathVariable String systemId){
        var system = solarSystemService.findSystemWithFullAccess(systemId);
        if(system == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"You have no access on changing permissions on this system");
        }
        return Converter.convertListManagesToManagerDTO(managerService.deleteManager(system,managerId));
    }

    @GetMapping("/newToken/{id}")
    public NewTokenDTO newToken(@PathVariable String id) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return solarSystemService.createNewToken(solarSystem);
    }

    @GetMapping("/statistics/{id}")
    public void updateStatistics(@PathVariable String id){
        var solarSystem = solarSystemService.findSystemWithMangeAccess(id);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        if(!influxTaskService.runInitial(solarSystem)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN ,"This calculation is only allowed once a day try tomorrow");
        }
    }

    @PutMapping("/status/{id}")
    public BooleanStatusTDO setBooleanStatus(@PathVariable String id,@RequestParam String name){
        var solarSystem = solarSystemService.findSystemWithMangeAccess(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        return statusService.addStatus(name,false, solarSystem);
    }

    @DeleteMapping("/status/{id}")
    public void deleteBooleanStatus(@PathVariable String id,@RequestParam String name){
        var solarSystem = solarSystemService.findSystemWithMangeAccess(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        statusService.removeStatus(name, solarSystem);
    }

    @PostMapping("/status/{id}")
    public BooleanStatusTDO setBooleanStatus(@PathVariable String id,@RequestParam String name,@RequestParam Boolean value){
        var solarSystem = solarSystemService.findSystemWithMangeAccess(id);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        StatusController.validateStatusName(name);

        return statusService.setStatus(name,value, solarSystem);
    }

}