package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.configuration.TaskSchedulerConfiguration;
import de.tostsoft.solarmonitoring.app.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.app.dtos.status.BooleanStatusTDO;
import de.tostsoft.solarmonitoring.app.service.*;
import de.tostsoft.solarmonitoring.lib.dto.PagedResponse;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.tostsoft.solarmonitoring.app.Converter.convertListManagesToManagerDTO;
import static de.tostsoft.solarmonitoring.lib.utils.MyStringUtils.quoteRegExSpecialChars;


@RestController//needs to be restController even if some restquest not rest
@Validated
@RequestMapping("/api/system")
public class SolarSystemController {

    @Autowired
    private StatusController statusController;

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

    @Autowired
    private TaskSchedulerConfiguration taskSchedulerConfiguration;

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

    public void validateAndFixSolarSystemDTO(EditSolarSystemDTO dto){
        dto.getSystemInformations().setName(validateName(dto.getSystemInformations().getName(),()->"Name dose not match requirements"));
        if(dto.getSystemInformations().getPublicName() != null){
            dto.getSystemInformations().setPublicName(validateName(dto.getSystemInformations().getPublicName(),()->"PublicName dose not match requirements"));
        }
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


        if(dto.getSystemInformations().getElectricityPrice() != null && dto.getSystemInformations().getElectricityPrice() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ElectricityPrice can not be negative");
        }

        if(dto.getSystemInformations().getElectricityPriceFeedIn() != null && dto.getSystemInformations().getElectricityPriceFeedIn() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ElectricityPriceFeedIn can not be negative");
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

    @PostMapping
    public EditSolarSystemDTO newSolar(@RequestBody @Valid EditSolarSystemDTO registerSolarSystemDTO) {

        validateAndFixSolarSystemDTO(registerSolarSystemDTO);

        return solarSystemService.createSystem(registerSolarSystemDTO);
    }

    @PostMapping("/edit")
    public ManagesSolarSystemDTO patchSolarSystem(@RequestBody @Valid EditSolarSystemDTO newSolarSystemDTO) {

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
        // Graph visibility is now controlled by graphFilter in ViewData
        // No need to set flags based on system type

        return returnDTO;
    }

    @GetMapping("/edit/{systemID}")
    public EditSolarSystemDTO getSystemForEdit(@PathVariable String systemID) {
        var solarSystem = solarSystemService.findSystemWithMangeAccess(systemID);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have edit access to this system");
        }

        var ret = Converter.convertSystemToEditResponseDTO(solarSystem);
        //TODO write some test for addional sets with permissions
        ret.setStatus(statusController.getAllStatusInternal(solarSystem));
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isOwner = solarSystem.getOwnedBy().getId().equals(user.getId());
        boolean isAdminManager = solarSystem.getManagedBy().stream()
                .anyMatch(m -> m.getPermission() == Permissions.ADMIN && StringUtils.equals(m.getUser().getId(), user.getId()));

        if (isOwner) {
            ret.setManagers(convertListManagesToManagerDTO(solarSystem.getManagedBy()));
        }
        if (isOwner || isAdminManager) {
            ret.setTokens(solarSystemService.listAccessTokens(solarSystem));
        }
        return ret;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSystem(@PathVariable String id) {
        var solarSystem = solarSystemService.findSystemWithOwnedBy(id);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have edit access to this system");
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
        if(StringUtils.equals(system.getOwnedBy().getId(), managerOpt.get().getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cann not add yourself as manager");
        }
        return convertListManagesToManagerDTO(managerService.addOrUpdateManageUser(system,addManagerDTO,managerOpt.get()));
    }

    @GetMapping("/allManager/{systemId}")
    public List<ManagerDTO> getManagers(@PathVariable String systemId) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId);
        if(solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Its nor your system");
        }
        return convertListManagesToManagerDTO(solarSystem.getManagedBy());
    }

    @PostMapping("/deleteManager/{managerId}/{systemId}")
    public  List<ManagerDTO> deleteManager(@PathVariable String managerId, @PathVariable String systemId){
        var system = solarSystemService.findSystemWithFullAccess(systemId);
        if(system == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"You have no access on changing permissions on this system");
        }
        return convertListManagesToManagerDTO(managerService.deleteManager(system,managerId));
    }

    @PostMapping("/tokens/{systemId}")
    public CreatedAccessTokenResponseDTO createToken(@PathVariable String systemId, @RequestBody @Valid CreateAccessTokenDTO dto) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found or no access");
        }
        dto.setName(validateName(dto.getName(), () -> "Token name does not match requirements"));
        return solarSystemService.createAccessToken(solarSystem, dto);
    }

    @DeleteMapping("/tokens/{systemId}/{tokenId}")
    public void deleteToken(@PathVariable String systemId, @PathVariable String tokenId) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found or no access");
        }
        solarSystemService.deleteAccessToken(solarSystem, tokenId);
    }

    @PatchMapping("/tokens/{systemId}/{tokenId}")
    public Object updateToken(@PathVariable String systemId, @PathVariable String tokenId, @RequestBody @Valid UpdateAccessTokenDTO dto) {
        var solarSystem = solarSystemService.findSystemWithFullAccess(systemId);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found or no access");
        }
        dto.setName(validateName(dto.getName(), () -> "Token name does not match requirements"));
        return solarSystemService.updateAccessToken(solarSystem, tokenId, dto);
    }

    @PostMapping("/statistics/{id}")
    public void updateStatistics(@PathVariable String id){
        var solarSystem = solarSystemService.findSystemWithMangeAccess(id);
        if (solarSystem == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not your system");
        }
        if(!influxTaskService.runInitial(solarSystem,taskSchedulerConfiguration.recalculateStatisticsThreadPool())){
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Its nor your system");
        }

        var tag = tagService.getTag(tagId);
        if(tag == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tga not found");
        }

        if(tag.getLocked() && !userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to set this tag");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Its nor your system");
        }

        var tag = tagService.getTag(tagId);
        if(tag == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }

        if(tag.getLocked() && !userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to remove this tag");
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

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/search")
    public PagedResponse<SolarSystemListItemDTO> search(@RequestBody SolarSystemSearchDTO searchDTO) {
        Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "creationDate", "buildingDate");

        var tagIds = new ArrayList<ObjectId>();
        if(!CollectionUtils.isEmpty(searchDTO.getTags())){
            for (String tag : searchDTO.getTags()) {
                ObjectId tagId;
                try{
                    tagId = new ObjectId(tag);
                }catch (IllegalArgumentException e){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag id: "+tag+" is not valid");
                }
                tagIds.add(tagId);
            }
        }


        var crit = new Criteria();

        var user = userService.getLoggedInUserFullNoException();
        var publicCrit = Criteria.where("publicMode").exists(true).ne(PublicMode.NONE);
        if(user == null){
            crit.andOperator(publicCrit);
        }else{

            var ownCrit = Criteria.where("ownedBy").is(user);
            var managesCrit = Criteria.where("id").in(user.getManges().stream().map(m->m.getSolarSystem().getId()).collect(Collectors.toList()));

            var accesCriteria = new Criteria();
            if(searchDTO.getIsPublic() == Boolean.TRUE){
                accesCriteria.orOperator(ownCrit,managesCrit,publicCrit);
            }else{
                accesCriteria.orOperator(ownCrit,managesCrit);
            }
            crit.andOperator(accesCriteria);
        }

        if(!CollectionUtils.isEmpty(tagIds)){
            crit.and("tags").in(tagIds);
        }

        if(searchDTO.getType() != null){
            crit.and("type").is(searchDTO.getType());
        }

        if(searchDTO.getName() != null){

            if(searchDTO.getName().length() < 3){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search name must have at least 3 characters");
            }
            var reg = quoteRegExSpecialChars(searchDTO.getName().toLowerCase());
            crit.and("systemInformations.name").regex(reg);
        }

        var overAllCrit = new Criteria();

        overAllCrit.andOperator(crit).and("deletedAt").isNull();

        int page = searchDTO.getPage() != null ? searchDTO.getPage() : 0;
        int size = searchDTO.getSize() != null ? searchDTO.getSize() : 15;
        String sortBy = searchDTO.getSortBy() != null ? searchDTO.getSortBy() : "name";
        String sortOrder = searchDTO.getSortOrder() != null ? searchDTO.getSortOrder() : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid sort field. Allowed fields: " + String.join(", ", ALLOWED_SORT_FIELDS));
        }

        if(StringUtils.equals(sortBy,"name")){
            sortBy = "systemInformations.name";
        }
        if(StringUtils.equals(sortBy,"buildingDate")){
            sortBy = "systemInformations.buildingDate";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        long totalElements = mongoTemplate.count(new Query(overAllCrit), SolarSystem.class);

        Query query = new Query(overAllCrit).with(pageable);
        List<SolarSystem> solarSystems = mongoTemplate.find(query, SolarSystem.class);

        List<SolarSystemListItemDTO> content = new ArrayList<>();
        for (SolarSystem solarSystem : solarSystems) {
            content.add(solarSystemService.solarSystemToListItemDTO(solarSystem, null));
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PagedResponse.<SolarSystemListItemDTO>builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build();
    }
}