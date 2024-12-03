package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.TagRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TagService {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private TagRepository tagRepository;

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

    public List<Pair<Tag,List<SolarSystem>>> getStartPageSystemsByTag(User user){

        List<Pair<Tag,List<SolarSystem>>> systemsByTags = new ArrayList<>();

        var tags = tagRepository.findAllByShowOnStartPage(true);

        for(var tag : tags){
            List<SolarSystem> systems;
            if(user == null){
                systems = solarSystemRepository.findAllByTagsContainsAndPublicModeIsNot(tag, PublicMode.NONE);
            }else{
                //TODO find better way to to this
                systems = solarSystemRepository.findAllByTagsContains(tag);
                //filter non access
                systems.stream().filter(s->
                        StringUtils.equals(s.getOwnedBy().getId(),user.getId()) ||
                        user.getManges().stream().anyMatch(ms->StringUtils.equals(s.getId(),ms.getSolarSystem().getId()))
                ).forEach(s->{});
            }
            if(!systems.isEmpty()){
                systemsByTags.add(new ImmutablePair<>(tag,systems));
            }
        }

        return systemsByTags;
    }
}
