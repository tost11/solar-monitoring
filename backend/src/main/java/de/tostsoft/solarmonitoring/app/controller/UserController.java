package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.GenericDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UpdateUserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserTableRowForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.*;
import de.tostsoft.solarmonitoring.app.service.NotificationService;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.app.service.ConfigService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.validation.Valid;
import kotlin.text.Regex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@Validated
@RequestMapping("/api/user")
public class UserController {

    Pattern pattern = Pattern.compile("^(.+)@(\\S+)$");

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        if (StringUtils.isBlank(userLoginDTO.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is empty");
        }
        if (StringUtils.isBlank(userLoginDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is empty");
        }
        var userDTO = userService.loginUser(userLoginDTO);

        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {

        if(!configService.isRegistrationEnabled()){
            throw new ResponseStatusException(HttpStatus.SEE_OTHER,"Registration currently disabled");
        }

        boolean requestIsValid = true;
        String responseMessage = "";

        userRegisterDTO.setName(StringUtils.trim(userRegisterDTO.getName()));

        Pattern p = Pattern.compile("^[A-Za-z0-9_-äüöÄÜÖßé]{3,30}$");
        Matcher m = p.matcher(userRegisterDTO.getName());
        if(!m.matches()) {
            LOG.error("User cant not Created because of Illegal characters");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"illegal characters");
        }
        if (StringUtils.length(userRegisterDTO.getName()) < 4) {
            requestIsValid = false;
            responseMessage += "\n Username must contain at least 4 characters";
        } else {
            if (userService.checkUsernameAlreadyTaken(userRegisterDTO)) {
                LOG.error("User is allredy used");
                requestIsValid = false;
                responseMessage += "\n Username is already taken";
            }
        }
        if (StringUtils.isEmpty(userRegisterDTO.getPassword())) {
            requestIsValid = false;
            responseMessage += "\n No password has been entered";

        } else if (userRegisterDTO.getPassword().length() < 8) {
            requestIsValid = false;
            responseMessage += "\n Password must contain at least 8 characters";
        }

        if (!requestIsValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
        }


        var userDTO = userService.registerUser(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    //endpoint only allowed to called by admins to change user settings
    @PostMapping("/admin/edit")
    public UserForAdminDTO editUser(@RequestBody UpdateUserForAdminDTO userDTO) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action not permitted");
        }
        return userService.editUser(userDTO);
    }

    //TODO refactor in other controller
    @GetMapping("/admin/findUser/{name}")
    public Collection<UserTableRowForAdminDTO> findUserForAdmins(@PathVariable String name) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getIsAdmin()) {
           return userService.findUserForAdmin(name);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action not permitted");
    }

    @GetMapping("/findUser/{name}")
    public List<GenericDataDTO> findUser(@PathVariable String name) {
        return userService.findUsers(name);
    }

    @DeleteMapping("/notification")
    public void deleteNotification(@RequestParam String id){
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        notificationService.deleteNotification(user,id);
    }

    @PostMapping("/notification")
    public NotificationDTO createNotification(@RequestBody @Valid CreateNotificationDTO notificationDTO){
        var user = userService.getLoggedInUserFull();

        //validation
        if(notificationDTO.getType() == NotificationType.Mail){
            var reg = new Regex(pattern);
            if(!reg.matches(notificationDTO.getValue())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a valid mail");
            }
        }else if(notificationDTO.getType() == NotificationType.UserMail){
            notificationDTO.setValue(null);
        }

        SolarSystem solarSystem = null;
        for (SolarSystem sys : user.getOwns()) {
            if(sys.getId().equals(notificationDTO.getId())){
                solarSystem = sys;
                break;
            }
        }

        if(solarSystem == null) {
            for (SolarSystem sys : user.getManges().stream().filter(s -> s.getPermission() == Permissions.ADMIN || s.getPermission() == Permissions.MANAGE).map(Manages::getSolarSystem).toList()) {
                if(sys.getId().equals(notificationDTO.getId())){
                    solarSystem = sys;
                    break;
                }
            }
        }

        if(solarSystem == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No Permission to add notification on that system");
        }

        var notification = notificationService.createNotification(notificationDTO.getType(),notificationDTO.getValue(),solarSystem,user);
        return Converter.converterToNotificationDTO(notification);
    }

    @GetMapping
    public UserDTO getOwnUser(){

        var user = userService.getLoggedInUserFull();

        var userDTO = Converter.converterUserToUserDTO(user);

        for (SolarSystem sys : user.getOwns()) {
            userDTO.getAccessSystems().add(Converter.converterSystemToUserAccessSystem(sys));
        }

        for (SolarSystem sys : user.getManges().stream().filter(s->s.getPermission() == Permissions.ADMIN || s.getPermission() == Permissions.MANAGE).map(Manages::getSolarSystem).toList()) {
            userDTO.getAccessSystems().add(Converter.converterSystemToUserAccessSystem(sys));
        }

        return userDTO;
    }

    @PostMapping
    public void getOwnUser(@RequestBody UpdateUserDTO updateUserDTO){

        var reg = new Regex(pattern);
        if(updateUserDTO.getMail() != null && !reg.matches(updateUserDTO.getMail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a valid mail");
        }

        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userRepository.updateMailByUserId(user.getId(),updateUserDTO.getMail());
    }


}
