package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.repository.TagRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TagService {


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

        tag = tagRepository.save(tag);

        return tag;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
}
