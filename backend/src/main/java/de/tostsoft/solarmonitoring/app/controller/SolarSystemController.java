package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ManagesSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.MultSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NamingsDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NewTokenDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PatchSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PublicSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.service.*;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
    private final Pattern namePatternShortener = Pattern.compile("^[A-Za-z0-9]{2,8}$");
    private final Pattern numberPattern = Pattern.compile("^[0-9]*$");
    @Autowired
    private TagService tagService;
    @Autowired
    private UserService userService;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    private String validateDeyeSunSerialNumbers(String serials){
        if(serials == null){
            return null;
        }
        Set<Long> numbers = new HashSet<>();
        var arr = StringUtils.split(serials,",");
        for (String serialString : arr) {
            var s = StringUtils.trim(serialString);
            if(StringUtils.isEmpty(s)){
                continue;
            }
            try{
                numbers.add(Long.parseLong(s));
            }catch (Exception exception){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"One Deye Sun serial is not Numeric");
            }
        }
        if(numbers.isEmpty()){
            return null;
        }

        return StringUtils.joinWith(",",numbers.stream().map(Object::toString).toArray());
    }

    public void validateAndFixSolarSystemDTO(RegisterSolarSystemDTO dto){
        dto.setName(validateName(dto.getName(),()->"Name dose not match requirements"));
        var trimmedShortner = validateName(dto.getShortener(),namePatternShortener,true,()->"Shortner dose not match requirements");
        if(StringUtils.isBlank(trimmedShortner)){
            dto.setShortener(null);
        }else{
            dto.setShortener(trimmedShortner);
        }
        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
        validateNamings(dto.getNamings());
        dto.setDeyeSunSerialNumbers(validateDeyeSunSerialNumbers(dto.getDeyeSunSerialNumbers()));

        if(dto.getElectricityPrice() != null && dto.getElectricityPrice() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ElectricityPrice can not be negative");
        }
    }

    private interface Runner{
        String run();
    }


    public String validateName(String name,Pattern pattern,boolean nullOk,Runner run){
        if(name == null){
            return null;
        }
        var trimmedName = StringUtils.trim(name);
        Matcher m = pattern.matcher(trimmedName);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,run.run());
        }
        return trimmedName;
    }


    public String validateName(String name,Runner run){
        var trimmedName = StringUtils.trim(name);
        Matcher m = namePattern.matcher(trimmedName);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,run.run());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"device ID naming not numeric");
        }
        try{
            Long.parseLong(name);
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"device ID naming number to large");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"input,output or battery ID naming not numeric");
            }
        }
        try{
            Long.parseLong(arr[0]);
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"device id on input,output or battery ID naming to large");
        }
        try{
            Integer.parseInt(arr[1]);
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"input,output or battery ID naming to large");
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
        dto.setName(validateName(dto.getName(),()->"Name dose not match requirements"));
        var trimmedShortner = validateName(dto.getShortener(),namePatternShortener,true,()->"Shortner dose not match requirements");
        if(StringUtils.isBlank(trimmedShortner)){
            dto.setShortener(null);
        }else{
            dto.setShortener(trimmedShortner);
        }

        //validate timezone
        TimeZone.getTimeZone(dto.getTimezone());
        validateNamings(dto.getNamings());
        dto.setDeyeSunSerialNumbers(validateDeyeSunSerialNumbers(dto.getDeyeSunSerialNumbers()));

        if(dto.getElectricityPrice() != null && dto.getElectricityPrice() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ElectricityPrice can not be negative");
        }
    }

    @PostMapping
    public RegisterSolarSystemResponseDTO newSolar(@RequestBody @Valid RegisterSolarSystemDTO registerSolarSystemDTO) {

        validateAndFixSolarSystemDTO(registerSolarSystemDTO);

        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/edit")
    public ManagesSolarSystemDTO patchSolarSystem(@RequestBody @Valid PatchSolarSystemDTO newSolarSystemDTO) {

        validateAndFixSolarSystemDTO(newSolarSystemDTO);

        var solarSystem = solarSystemService.findSystemWithMangeAccess(newSolarSystemDTO.getId());
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        return solarSystemService.patchSolarSystem(newSolarSystemDTO, solarSystem);
    }

    @GetMapping("/public/{systemID}")
    public PublicSolarSystemDTO getSystemPublic(@PathVariable String systemID) {
        return getSystem(systemID);
    }

    @GetMapping("/{systemID}")
    public PublicSolarSystemDTO getSystem(@PathVariable String systemID) {
        var pair = solarSystemService.getSystemWithUserFromContextOrPublic(systemID);
        if(pair == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }
        var returnDTO = pair.getLeft();
        var system = pair.getRight();

        if(system.getType() == SolarSystemType.GRID){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(true);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(system.getType() == SolarSystemType.GRID_BATTERY){
            returnDTO.getViewData().setHasACInput(true);
            returnDTO.getViewData().setHasACOutput(true);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(system.getType() == SolarSystemType.SIMPLE){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(false);
            returnDTO.getViewData().setHasDCOutput(false);
        }else if(system.getType() == SolarSystemType.VERY_SIMPLE){
            returnDTO.getViewData().setHasACInput(false);
            returnDTO.getViewData().setHasACOutput(false);
            returnDTO.getViewData().setHasDCOutput(false);
        }

        return returnDTO;
    }

    @GetMapping("/all")
    public Collection<SolarSystemListItemDTO> getSystems(@RequestParam(value = "public",required = false) Boolean showPublic) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = auth != null && auth.isAuthenticated() ? (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;

        Map<String,SolarSystemListItemDTO> res = new HashMap<>();

        if(showPublic){
            for (SolarSystemListItemDTO solarSystemListItemDTO : solarSystemService.getPublicSystems()) {
                res.put(solarSystemListItemDTO.getId(),solarSystemListItemDTO);
            }
        }

        if(user != null){
            for (SolarSystemListItemDTO solarSystemListItemDTO : solarSystemService.getSystemsWithUserFromContext()) {
                res.put(solarSystemListItemDTO.getId(),solarSystemListItemDTO);
            }
        }

        return res.values();
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

    @GetMapping("/public/mult")
    public List<MultSolarSystemDTO> getSystemMultPublic(@RequestParam String[] systemIds) {
        return getSystemMult(systemIds);
    }

    @GetMapping("/mult")
    public List<MultSolarSystemDTO> getSystemMult(@RequestParam String[] systemIds) {
        if(systemIds.length==0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one system has to be specified to be shown");
        }
        if(systemIds.length > 10){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum of multiple Systems to show is 10");
        }
        var pairs = solarSystemService.findSolarSystemsByWithAccess(Arrays.stream(systemIds).toList());
        //TODO maby fix public mode stuff
        return Converter.convertSystemsToMultSolarSystemDTOs(pairs.stream().map(Pair::getKey).collect(Collectors.toList()));
    }

    @PostMapping("/tag")
    public void addTagToSystem(@RequestParam String systemId,@RequestParam String tagId) {
        var solarSystem = solarSystemService.findSystemWithMangeAccess(systemId);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        var tag = tagService.getTag(tagId);
        if(tag == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tga not found");
        }

        if(tag.getLocked() && !userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not allowed to set this tag");
        }

        for (Tag solarSystemTag : solarSystem.getTags()) {
            if(StringUtils.equals(solarSystemTag.getId(),tag.getId())){
                return;
            }
        }
        solarSystem.getTags().add(tag);
        solarSystemRepository.save(solarSystem);

        //TODO find way to do this (here no reference is used)
        //solarSystemRepository.saveTags(solarSystem.getId(),solarSystem.getTags());
    }


    @DeleteMapping("/tag")
    public void removeTagToSystem(@RequestParam String systemId,@RequestParam String tagId) {
        var solarSystem = solarSystemService.findSystemWithMangeAccess(systemId);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }

        var tag = tagService.getTag(tagId);
        if(tag == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }

        if(tag.getLocked() && !userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not allowed to remove this tag");
        }

        boolean found = false;
        for (Tag solarSystemTag : solarSystem.getTags()) {
            if(StringUtils.equals(solarSystemTag.getId(),tag.getId())){
                found = true;
                break;
            }
        }

        if(!found){
            return;
        }

        solarSystem.getTags().removeIf((t)->StringUtils.equals(t.getId(),tagId));
        solarSystemRepository.save(solarSystem);

        //TODO find way to do this (here no reference is used)
        //solarSystemRepository.saveTags(solarSystem.getId(),solarSystem.getTags());
    }
}