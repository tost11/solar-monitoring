package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.GenericDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.EditUserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.*;
import de.tostsoft.solarmonitoring.app.service.CaptchaService;
import de.tostsoft.solarmonitoring.app.service.NotificationService;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.app.service.ConfigService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.repository.JWTSessionTokenRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.validation.Valid;
import kotlin.text.Regex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.bson.types.ObjectId;
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

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JWTSessionTokenRepository jwtSessionTokenRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {

        userLoginDTO.setName(StringUtils.trim(StringUtils.toRootLowerCase(userLoginDTO.getName())));

        if (StringUtils.isBlank(userLoginDTO.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is empty");
        }
        if (StringUtils.isBlank(userLoginDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is empty");
        }
        var userDTO = userService.loginUser(userLoginDTO);

        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @GetMapping("/register")
    public RegisterInfoDTO getRegisterInfo(){

        return RegisterInfoDTO.builder()
                .captcha(captchaService.generageCaptcha().getBase64Image())
                .build();
    }


    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {

        if(!configService.isRegistrationEnabled()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Registration currently disabled");
        }

        if(configService.limitRegistrationReached()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Daily user registration limit reached");
        }

        boolean requestIsValid = true;
        String responseMessage = "";

        userRegisterDTO.setName(StringUtils.trim(userRegisterDTO.getName()));
        userRegisterDTO.setPassword(StringUtils.trim(userRegisterDTO.getPassword()));
        userRegisterDTO.setMail(StringUtils.trim(userRegisterDTO.getMail()).toLowerCase());

        if(StringUtils.isEmpty(userRegisterDTO.getMail())){
            requestIsValid = false;
            responseMessage += "\nMail cant not Created because of mail is empty";
        }else{
            if(!EmailValidator.getInstance().isValid(userRegisterDTO.getMail())){
                requestIsValid = false;
                responseMessage += "\nMail cant not Created because of mail is invalid";
            }else{
                if (userService.checkUserMailAlreadyTaken(userRegisterDTO)) {
                    requestIsValid = false;
                    responseMessage += "\nMail is already taken";
                }
            }
        }

        Pattern p = Pattern.compile("^[A-Za-z0-9_\\-äüöÄÜÖßé]{3,30}$");
        Matcher m = p.matcher(userRegisterDTO.getName());
        if(!m.matches()) {
            requestIsValid = false;
            responseMessage += "\nUsername cant not Created because of Illegal characters";
        }else if(StringUtils.length(userRegisterDTO.getName()) < 4) {
            requestIsValid = false;
            responseMessage += "\nUsername must contain at least 4 characters";
        } else if (userService.checkUsernameAlreadyTaken(userRegisterDTO)) {
            requestIsValid = false;
            responseMessage += "\nUsername is already taken";
        }

        String passwordRegex = "^(?=.*[A-ZÄÜÖ])(?=.*[a-zäüöß])(?=.*\\d)(?=.*[@$%*#?!&])[A-ZÄÖÜa-zäöüß\\d@$!%*#?&]{10,64}$";

        Pattern pattern = Pattern.compile(passwordRegex);
        Matcher matcher = pattern.matcher(userRegisterDTO.getPassword());

        if (!matcher.matches()) {
            requestIsValid = false;
            responseMessage += "\nPassword not matching criteria: between 8 and 64 characters, one upper case letter, one lower case letter, one number, one of therese characters: ?=.*[@#$%^&+=]";
        }

        var captcha = captchaService.getCaptchaByByBase64Image(userRegisterDTO.getCaptcha());
        if(captcha == null) {
            requestIsValid = false;
            responseMessage += "\nCaptcha unknown (please reload image)";
        }else if (!StringUtils.equalsAnyIgnoreCase(captcha.getText(), userRegisterDTO.getCaptchaText())) {
            requestIsValid = false;
            responseMessage += "\nCaptcha not answered correct";
        }

        //this response modes is used so multiple response messages can be send back
        if (!requestIsValid) {
            LOG.debug("User not createde because of: \n"+responseMessage);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
        }

        userService.registerUser(userRegisterDTO);

        captchaService.deleteCaptcha(captcha);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    //endpoint only allowed to called by admins to change user settings
    @PostMapping("/admin/edit")
    public UserForAdminDTO editUser(@RequestBody EditUserForAdminDTO userDTO) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action not permitted");
        }
        return userService.editUser(userDTO);
    }

    //TODO refactor in other controller
    @GetMapping("/admin/findUser/{name}")
    public Collection<UserForAdminDTO> findUserForAdmins(@PathVariable String name) {
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
            if(!EmailValidator.getInstance().isValid(notificationDTO.getValue())){
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


    @DeleteMapping
    void deleteOwnUser(){
        var user = userService.getLoggedInUserFull();

        if(user == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"User not logged in");
        }
        userService.deleteUserWithAllSystemsAnRelations(user);

        jwtSessionTokenRepository.deleteAllByOwnedByIs(user);
    }


    //TODO fix user safe with new mail verification
    /*
    @PostMapping
    public void updateOwnUser(@RequestBody UpdateUserDTO updateUserDTO){

        var reg = new Regex(pattern);
        if(updateUserDTO.getMail() != null && !reg.matches(updateUserDTO.getMail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a valid mail");
        }

        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userRepository.updateMailByUserId(user.getId(),updateUserDTO.getMail());
    }*/

    @GetMapping("/activate/{id}")
    public ResponseEntity<String> activateAccount(@PathVariable String id){
        //if this isnt working execption is thrown
        if(!ObjectId.isValid(id)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid activation link");
        }

        var user = userService.activateUser(id);

        return ResponseEntity.status(HttpStatus.OK).body("<html><title>user activated</title><body><h4>user crated</h4>the user: "+user.getViewName()+" was activated go to start page and log in!</body></html>");

    }

    @PostMapping("/logout")
    public void signOut(){
        userService.signOutCurrentContext();
    }
}
