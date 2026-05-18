package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.TagRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private SolarSystemService solarSystemService;

    private List<Pair<Tag,List<SolarSystem>>> cachedPublicSystemsByTag;
    private Instant cachedPublicSystemsByTagUpdated;
    @Value("${tag.cache.time:60}")
    private int cachedPublicSystemsByTagTime;

    public Tag createTag(Tag tag) {
        if(tagRepository.countByName(tag.getName()) > 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tag with this name already exists");
        }
        return tagRepository.save(tag);
    }

    public Tag editTag(Tag editTag) {
        var tagOpt = tagRepository.findById(editTag.getId());
        if(tagOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Status with this id not found");
        }
        var tag = tagOpt.get();
        if(!StringUtils.equals(editTag.getName(),tag.getName())){
            if(tagRepository.countByName(editTag.getName()) > 0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tag with this name already exists");
            }
        }

        tag.setName(editTag.getName());
        tag.setViewName(editTag.getViewName());
        tag.setLocked(editTag.getLocked());
        tag.setColor(editTag.getColor());
        tag.setShowOnStartPage(editTag.getShowOnStartPage());

        tag = tagRepository.save(tag);

        return tag;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public List<Tag> getAllTagsById(Set<String> tags){
        if(CollectionUtils.isEmpty(tags)){
            return new ArrayList<>();
        }
        var ret = tagRepository.findAllById(tags);
        if(ret.size() != tags.size()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Some tags are unknown");
        }
        return ret;
    }

    public Tag getTag(String id){
        return tagRepository.findById(id).orElse(null);
    }

    //this is used to reduce load on start page
    public synchronized List<Pair<Tag, List<SolarSystem>>> getPublicSystemsByTag(){
        if(cachedPublicSystemsByTagUpdated == null || cachedPublicSystemsByTagUpdated.plus(cachedPublicSystemsByTagTime, ChronoUnit.SECONDS).isBefore(Instant.now())){

            var tmpResult = new ArrayList<Pair<Tag, List<SolarSystem>>>();

            var tags = tagRepository.findAllByShowOnStartPage(true);
            for(var tag : tags){
                List<SolarSystem> systems;
                //unmodifiable so no changes possible and not thread executions if cached
                systems = Collections.unmodifiableList(solarSystemRepository.findAllByTagsContainsAndPublicModeIsNot(tag.getId(), PublicMode.NONE));
                if(!systems.isEmpty()){
                    tmpResult.add(new ImmutablePair<>(tag,systems));
                }
            }

            cachedPublicSystemsByTag = Collections.unmodifiableList(tmpResult);

            cachedPublicSystemsByTagUpdated = Instant.now();
        }
        return cachedPublicSystemsByTag;
    }

    public List<Pair<Tag,List<SolarSystem>>> getStartPageSystemsByTag(User user){

        if(user == null){
            return getPublicSystemsByTag();
        }

        List<Pair<Tag,List<SolarSystem>>> systemsByTags = new ArrayList<>();

        var tags = tagRepository.findAllByShowOnStartPage(true);

        for(var tag : tags){
            List<SolarSystem> systems;
            //TODO find better way to to this
            systems = solarSystemRepository.findAllByTagsContains(tag.getId());
            //filter non access
            systems.stream().filter(s->
                    StringUtils.equals(s.getOwnedBy().getId(),user.getId()) ||
                    user.getManges().stream().anyMatch(ms->StringUtils.equals(s.getId(),ms.getSolarSystem().getId()))
            ).forEach(s->{});
            if(!systems.isEmpty()){
                systemsByTags.add(new ImmutablePair<>(tag,systems));
            }
        }

        return systemsByTags;
    }

    public Pair<Tag, List<Pair<SolarSystem, PublicMode>>> findTagWithAccessibleSystems(String tagId) {
        var tagOpt = tagRepository.findById(tagId);
        if (tagOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        Tag tag = tagOpt.get();

        List<SolarSystem> systems = solarSystemRepository.findAllByTagsContains(tag.getId());

        if (systems.isEmpty()) {
            return new ImmutablePair<>(tag, new ArrayList<>());
        }

        List<String> systemIds = systems.stream().map(SolarSystem::getId).collect(Collectors.toList());
        List<Pair<SolarSystem, PublicMode>> accessibleSystems;

        try {
            accessibleSystems = solarSystemService.findSolarSystemsByWithAccess(systemIds);
        } catch (ResponseStatusException e) {
            accessibleSystems = new ArrayList<>();
        }

        return new ImmutablePair<>(tag, accessibleSystems);
    }
}
