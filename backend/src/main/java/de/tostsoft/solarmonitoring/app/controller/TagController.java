package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.tags.AdminTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.CreateTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.app.service.SolarSystemService;
import de.tostsoft.solarmonitoring.app.service.TagAggregationCacheService;
import de.tostsoft.solarmonitoring.app.service.TagService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import de.tostsoft.solarmonitoring.lib.dto.PagedResponse;
import de.tostsoft.solarmonitoring.lib.dto.SystemContributionDTO;
import de.tostsoft.solarmonitoring.lib.dto.TagAggregationDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.repository.TagRepository;
import org.apache.commons.lang3.StringUtils;
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
    @Autowired
    private TagAggregationCacheService tagAggregationCacheService;

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

        // Fetch cached aggregation data (expensive operations)
        TagAggregationCacheService.AggregationData data = tagAggregationCacheService.getAggregationData(id);

        if (data.getSystems().isEmpty()) {
            return ResponseEntity.ok(buildEmptyAggregationDTO(data.getTag()));
        }

        List<SystemContributionDTO> contributionDTOs = data.getSystems();

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
                .id(data.getTag().getId())
                .name(data.getTag().getName())
                .color(data.getTag().getColor())
                .build())
            .totalSystems(data.getSystems().size())
            .onlineSystems(data.getOnlineCount())
            .totalDayProducedKWH(data.getTotalDayProducedKWH())
            .totalDayConsumedKWH(data.getTotalDayConsumedKWH())
            .totalCurrentProduction(data.getTotalCurrentProduction())
            .totalCurrentConsumption(data.getTotalCurrentConsumption())
            .totalCurrentGrid(data.getTotalCurrentGrid())
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
