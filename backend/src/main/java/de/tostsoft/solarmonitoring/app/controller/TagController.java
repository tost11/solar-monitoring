package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.tags.AdminTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.CreateTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.app.service.TagService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

import static de.tostsoft.solarmonitoring.app.Converter.*;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private UserService userService;
    @Autowired
    private TagService tagService;

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
    public List<AdminTagDTO> getTags(){
        if(!userService.isUserFromContextAdmin()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have not permission to do that!");
        }
        var tags  = tagService.getAllTags();
        return tags.stream().map(Converter::convertTagToTAdminTagDTO).toList();
    }

    @GetMapping("/available")
    public List<TagDTO> getAvailableTags(){
        var isAdmin = userService.isUserFromContextAdmin();

        var tags  = tagService.getAllTags();
        if(!isAdmin){
            tags = tags.stream().filter(Tag::getLocked).toList();
        }

        return tags.stream().map(Converter::convertTagToTagDTO).toList();
    }
}
