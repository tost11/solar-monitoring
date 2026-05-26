package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.tags.AdminTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.CreateTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.app.service.SolarSystemService;
import de.tostsoft.solarmonitoring.app.service.TagService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import de.tostsoft.solarmonitoring.lib.dto.PagedResponse;
import de.tostsoft.solarmonitoring.lib.dto.SystemContributionDTO;
import de.tostsoft.solarmonitoring.lib.dto.TagAggregationDTO;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.repository.TagRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static de.tostsoft.solarmonitoring.app.Converter.*;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private static final Logger LOG = LoggerFactory.getLogger(TagController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private TagService tagService;
    @Autowired
    private SolarSystemService solarSystemService;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private InfluxService influxService;

    private final Pattern namePattern = Pattern.compile("^[A-Za-z0-9_\\-äüöÄÜÖßé ]{3,20}$");

    private final Pattern colorPattern = Pattern.compile("^#(?:[0-9a-fA-F]{3}){1,2}$");

    @PostMapping
    public AdminTagDTO addTag(@Validated @RequestBody CreateTagDTO tagDTO) {
        if(!userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have not permission to do that!");
        }

        tagDTO.setName(StringUtils.trim(tagDTO.getName()));
        tagDTO.setColor(StringUtils.trim(tagDTO.getColor()));

        if(!namePattern.matcher(tagDTO.getName()).matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name requires: 3-20 Leters, only normal letters, space and Numbers");
        }
        if(!colorPattern.matcher(tagDTO.getColor()).matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a valid hex color");
        }

        tagDTO.setColor(StringUtils.toRootLowerCase(tagDTO.getColor()));
        var tag = convertCreateTagDTOtoTag(tagDTO);

        if(tagDTO.getId() == null){
            return convertTagToTAdminTagDTO(tagService.createTag(tag));
        }else{
            return convertTagToTAdminTagDTO(tagService.editTag(tag));
        }
    }

    //TODO implement delete tag

    @GetMapping
    public List<? extends TagDTO> getTags(){
        var isAdmin = userService.isIfUserFromContextAdminNoException();
        var tags = tagService.getAllTags();

        if (isAdmin) {
            return tags.stream().map(Converter::convertTagToTAdminTagDTO).toList();
        } else {
            return tags.stream().map(Converter::convertTagToTagDTO).toList();
        }
    }

    @GetMapping("/available")
    public List<TagDTO> getAvailableTags(){
        var isAdmin = userService.isUserFromContextAdmin();

        var tags  = tagService.getAllTags();
        if(!isAdmin){
            tags = tags.stream().filter(t->!t.getLocked()).toList();
        }

        return tags.stream().map(Converter::convertTagToTagDTO).toList();
    }

    @GetMapping("/systems")
    public List<TagSolarSystemDTO> getStartPageTagsWithSystems(){
        var user = userService.getLoggedInUserFullNoException();

        var systemsByTags = tagService.getStartPageSystemsByTag(user);

        List<TagSolarSystemDTO> ret = new ArrayList<>();

        for(var systemsByTag : systemsByTags){
            TagSolarSystemDTO tagSolarSystemDTO = new TagSolarSystemDTO();
            tagSolarSystemDTO.setTag(Converter.convertTagToTagDTO(systemsByTag.getLeft()));
            tagSolarSystemDTO.setSystems(new ArrayList<>());
            for (SolarSystem solarSystem : systemsByTag.getRight()) {
                tagSolarSystemDTO.getSystems().add(solarSystemService.solarSystemToListItemDTO(solarSystem,user));
            }

            ret.add(tagSolarSystemDTO);
        }

        return ret;
    }

    @GetMapping("/byIds")
    public List<TagDTO> findById(@RequestParam(name = "ids") List<String> tagIds){
        var ret = new ArrayList<TagDTO>();
        if(CollectionUtils.isEmpty(tagIds)){
            return ret;
        }
        List<ObjectId> ids = new ArrayList<>();
        for (String tag : tagIds) {
            try{
                ids.add(new ObjectId(tag));
            }catch (Exception e){}
        }
        if(ids.isEmpty()){
            return ret;
        }
        return tagRepository.findAllByIdIn(ids).stream().map(Converter::convertTagToTagDTO).toList();
    }

    @GetMapping("/aggregation/{id}")
    public ResponseEntity<TagAggregationDTO> getTagAggregation(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        User user = userService.getLoggedInUserFullNoException();

        Pair<Tag, List<Pair<SolarSystem, PublicMode>>> tagAndSystems =
            tagService.findTagWithAccessibleSystems(id);

        Tag tag = tagAndSystems.getLeft();
        List<Pair<SolarSystem, PublicMode>> accessibleSystems = tagAndSystems.getRight();

        if (accessibleSystems.isEmpty()) {
            return ResponseEntity.ok(buildEmptyAggregationDTO(tag));
        }

        float totalDayProducedKWH = 0;
        float totalDayConsumedKWH = 0;
        float totalCurrentProduction = 0;
        float totalCurrentConsumption = 0;
        float totalCurrentGrid = 0;
        int onlineCount = 0;

        List<SystemContributionDTO> contributionDTOs = new ArrayList<>();

        for (Pair<SolarSystem, PublicMode> pair : accessibleSystems) {
            SolarSystem system = pair.getLeft();
            PublicMode publicMode = pair.getRight();

            boolean isOnline = system.isOnline();
            if (isOnline) {
                onlineCount++;
            }

            boolean showConsumption = publicMode == null || publicMode == PublicMode.ALL;
            String role = publicMode == null ? "ADMIN" : "PUBLIC";

            if (publicMode == null && user != null) {
                boolean isOwner = StringUtils.equals(system.getOwnedBy().getId(), user.getId());
                if (!isOwner) {
                    var managesOpt = system.getManagedBy().stream()
                        .filter(m -> StringUtils.equals(m.getUser().getId(), user.getId()))
                        .findFirst();
                    if (managesOpt.isPresent()) {
                        Permissions permission = managesOpt.get().getPermission();
                        role = permission.name();
                    }
                }
            }

            float dayProducedKWH = 0;
            Float dayConsumedKWH = null;
            Float dayConsumedBase = null;
            Float dayGridConsumed = null;
            Float dayGridFeedIn = null;

            ZoneId zoneId = ZoneId.of(system.getTimezone() == null ? "UTC" : system.getTimezone());
            LocalDate today = LocalDate.now(zoneId);
            ZonedDateTime startOfToday = today.atStartOfDay(zoneId);
            ZonedDateTime endOfToday = today.plusDays(1).atStartOfDay(zoneId).minusSeconds(1);
            Date fromDate = Date.from(startOfToday.toInstant());
            Date toDate = Date.from(endOfToday.toInstant());

            try {
                var fluxTables = influxService.getStatisticsDataAsJson(
                    system,
                    InfluxMeasurement.SOLAR_DAY_DATA,
                    fromDate,
                    toDate,
                    !showConsumption
                );

                if (fluxTables != null && !fluxTables.isEmpty()) {
                    // Extract all fields from FluxTables
                    // getStatisticsDataAsJson returns normalized field names ("Produced", "Consumed", etc.)
                    for (var table : fluxTables) {
                        for (var record : table.getRecords()) {
                            String field = (String) record.getValueByKey("_field");
                            Object value = record.getValue();

                            if (value instanceof Number) {
                                float floatValue = ((Number) value).floatValue();

                                if (StringUtils.equals(field, InfluxService.API_NAMING_PRODUCED)) {
                                    dayProducedKWH = Math.max(dayProducedKWH, floatValue);
                                } else if (showConsumption) {
                                    if (StringUtils.equals(field, InfluxService.API_NAMING_CONSUMED)) {
                                        dayConsumedBase = Math.max(dayConsumedBase != null ? dayConsumedBase : 0, floatValue);
                                    } else if (StringUtils.equals(field, InfluxService.API_NAMING_GRID_CONSUMPTION)) {
                                        dayGridConsumed = Math.max(dayGridConsumed != null ? dayGridConsumed : 0, floatValue);
                                    } else if (StringUtils.equals(field, InfluxService.API_NAMING_GRID_FEEDIN)) {
                                        dayGridFeedIn = Math.max(dayGridFeedIn != null ? dayGridFeedIn : 0, floatValue);
                                    }
                                }
                            }
                        }
                    }
                }

                // Calculate total day consumption based on showGridInfo setting
                boolean showGridInfo = system.getViewData() != null
                    && system.getViewData().getShowGridInfo() != null
                    && system.getViewData().getShowGridInfo();

                if (showGridInfo && dayConsumedBase != null) {
                    // Total household consumption = device output + grid consumption - grid feed-in
                    // When production > consumption, some energy is fed to grid, so actual household consumption is less
                    float totalConsumption = dayConsumedBase;
                    if (dayGridConsumed != null) {
                        totalConsumption += dayGridConsumed;
                    }
                    if (dayGridFeedIn != null) {
                        totalConsumption -= dayGridFeedIn;
                    }
                    dayConsumedKWH = Math.max(0, totalConsumption);
                } else if (dayConsumedBase != null) {
                    // Fallback to device output only when grid info not enabled or grid data unavailable
                    dayConsumedKWH = dayConsumedBase;
                }
            } catch (Exception e) {
                LOG.error("Error fetching statistics data for system {} in tag aggregation", system.getId(), e);
            }

            float currentProduction = 0;
            Float currentConsumption = null;
            Float currentGrid = null;

            if (system.getCurrentValues() != null && system.getCurrentValues().isFromToday(system.getTimezone())) {
                currentProduction = system.getCurrentValues().getInputWatt() != null
                    ? system.getCurrentValues().getInputWatt() : 0;

                if (showConsumption) {
                    Float outputWatt = system.getCurrentValues().getOutputWatt();
                    Float gridWatt = system.getCurrentValues().getGridWatt();

                    // Check if showGridInfo is enabled for this system
                    boolean showGridInfo = system.getViewData() != null
                        && system.getViewData().getShowGridInfo() != null
                        && system.getViewData().getShowGridInfo();

                    // Calculate total consumption when showGridInfo is enabled (consistent with daily calculation)
                    if (showGridInfo && gridWatt != null && outputWatt != null) {
                        // Total household consumption = device output + grid consumption
                        // Matches daily calculation: CalcConsumedKWH = ConsumedKWH + GridConsumedKWH
                        currentConsumption = Math.max(0, outputWatt + gridWatt);
                    } else if (outputWatt != null) {
                        // Fallback to device output only when grid info not enabled
                        currentConsumption = outputWatt;
                    }

                    currentGrid = gridWatt;
                }
            }

            totalDayProducedKWH += dayProducedKWH;
            if (dayConsumedKWH != null) {
                totalDayConsumedKWH += dayConsumedKWH;
            }
            if (isOnline) {
                totalCurrentProduction += currentProduction;
                if (currentConsumption != null) {
                    totalCurrentConsumption += currentConsumption;
                }
                if (currentGrid != null) {
                    totalCurrentGrid += currentGrid;
                }
            }

            contributionDTOs.add(SystemContributionDTO.builder()
                .id(system.getId())
                .name(system.getName())
                .type(system.getType() != null ? system.getType().name() : "UNKNOWN")
                .isOnline(isOnline)
                .dayProducedKWH(dayProducedKWH)
                .dayConsumedKWH(dayConsumedKWH)
                .currentProduction(currentProduction)
                .currentConsumption(currentConsumption)
                .currentGrid(currentGrid)
                .role(role)
                .maxInstalledSolarPower(system.getMaxInstalledSolarPower())
                .build());
        }

        // Sort the systems
        Comparator<SystemContributionDTO> comparator = getComparatorForSortField(sortBy);

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        // Separate systems with and without values for the sort field
        List<SystemContributionDTO> withValues = new ArrayList<>();
        List<SystemContributionDTO> withoutValues = new ArrayList<>();

        for (SystemContributionDTO dto : contributionDTOs) {
            if (hasValueForSortField(dto, sortBy)) {
                withValues.add(dto);
            } else {
                withoutValues.add(dto);
            }
        }

        // Sort systems with values
        withValues.sort(comparator);

        // Sort systems without values by name
        withoutValues.sort(Comparator.comparing(SystemContributionDTO::getName));

        // Combine: systems with values first, then systems without values
        contributionDTOs = new ArrayList<>();
        contributionDTOs.addAll(withValues);
        contributionDTOs.addAll(withoutValues);

        // Apply pagination
        int totalSystems = contributionDTOs.size();
        int totalPages = (int) Math.ceil((double) totalSystems / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalSystems);

        List<SystemContributionDTO> pagedSystems = contributionDTOs.subList(startIndex, endIndex);

        PagedResponse<SystemContributionDTO> pagedResponse = PagedResponse.<SystemContributionDTO>builder()
            .content(pagedSystems)
            .page(page)
            .size(size)
            .totalElements(totalSystems)
            .totalPages(totalPages)
            .build();

        TagAggregationDTO result = TagAggregationDTO.builder()
            .tag(de.tostsoft.solarmonitoring.lib.dto.TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build())
            .totalSystems(accessibleSystems.size())
            .onlineSystems(onlineCount)
            .totalDayProducedKWH(totalDayProducedKWH)
            .totalDayConsumedKWH(totalDayConsumedKWH)
            .totalCurrentProduction(totalCurrentProduction)
            .totalCurrentConsumption(totalCurrentConsumption)
            .totalCurrentGrid(totalCurrentGrid)
            .systems(pagedResponse)
            .build();

        return ResponseEntity.ok(result);
    }

    private TagAggregationDTO buildEmptyAggregationDTO(Tag tag) {
        PagedResponse<SystemContributionDTO> emptyPaged = PagedResponse.<SystemContributionDTO>builder()
            .content(new ArrayList<>())
            .page(0)
            .size(15)
            .totalElements(0)
            .totalPages(0)
            .build();

        return TagAggregationDTO.builder()
            .tag(de.tostsoft.solarmonitoring.lib.dto.TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build())
            .totalSystems(0)
            .onlineSystems(0)
            .totalDayProducedKWH(0)
            .totalDayConsumedKWH(0)
            .totalCurrentProduction(0)
            .totalCurrentConsumption(0)
            .totalCurrentGrid(0)
            .systems(emptyPaged)
            .build();
    }

    private Comparator<SystemContributionDTO> getComparatorForSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "dayproduction":
                return Comparator.comparing(SystemContributionDTO::getDayProducedKWH);
            case "dayconsumption":
                return Comparator.comparing(dto -> dto.getDayConsumedKWH() != null ? dto.getDayConsumedKWH() : 0f);
            case "currentproduction":
                return Comparator.comparing(SystemContributionDTO::getCurrentProduction);
            case "currentconsumption":
                return Comparator.comparing(dto -> dto.getCurrentConsumption() != null ? dto.getCurrentConsumption() : 0f);
            case "currentgrid":
                return Comparator.comparing(dto -> dto.getCurrentGrid() != null ? dto.getCurrentGrid() : 0f);
            case "efficiency":
                return Comparator.comparing(dto -> {
                    if (dto.getMaxInstalledSolarPower() != null && dto.getMaxInstalledSolarPower() > 0) {
                        return dto.getCurrentProduction() / dto.getMaxInstalledSolarPower();
                    }
                    return 0f;
                });
            case "name":
                return Comparator.comparing(SystemContributionDTO::getName);
            case "online":
                return Comparator.comparing(SystemContributionDTO::isOnline);
            default:
                return Comparator.comparing(SystemContributionDTO::getName);
        }
    }

    private boolean hasValueForSortField(SystemContributionDTO dto, String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "dayproduction":
                return dto.getDayProducedKWH() > 0;
            case "dayconsumption":
                return dto.getDayConsumedKWH() != null && dto.getDayConsumedKWH() > 0;
            case "currentproduction":
                return dto.getCurrentProduction() > 0;
            case "currentconsumption":
                return dto.getCurrentConsumption() != null && dto.getCurrentConsumption() > 0;
            case "currentgrid":
                return dto.getCurrentGrid() != null && dto.getCurrentGrid() != 0f;
            case "efficiency":
                return dto.getMaxInstalledSolarPower() != null && dto.getMaxInstalledSolarPower() > 0;
            case "name":
            case "online":
                return true;
            default:
                return true;
        }
    }
}
