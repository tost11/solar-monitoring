package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.GenericDataDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UpdateUserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserTableRowForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.service.UserService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody UserLoginDTO userLoginDTO) {
        if (StringUtils.isBlank(userLoginDTO.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is empty");
        }
        if (StringUtils.isBlank(userLoginDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is empty");
        }
        var userDTO = userService.loginUser(userLoginDTO);

        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    //TODO restrigt input of username to normal characters number and spaces
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserRegisterDTO userRegisterDTO) {
        boolean requestIsValid = true;
        String responseMessage = "";

        userRegisterDTO.setName(StringUtils.trim(userRegisterDTO.getName()));

        Pattern p = Pattern.compile("[a-zA-Z0-9äöüÄÖÜßé]*[a-zA-Z0-9äöüÄÖÜßé]");
        Matcher m = p.matcher(userRegisterDTO.getName());
        if(m.matches()) {
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"illegal characters");
    }

    //endpoint only allowed to called by admins to change user settings
    @PostMapping("/edit")
    public UserForAdminDTO editUser(@RequestBody UpdateUserForAdminDTO userDTO) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action not permitted");
        }
        return userService.editUser(userDTO);
    }

    //TODO refactor in other controller
    @GetMapping("/admin/findUser/{name}")
    public List<UserTableRowForAdminDTO> findUserForAdmins(@PathVariable String name) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getIsAdmin()) {
           return userService.findUserForAdmin(name);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action not permitted");
    }

    @GetMapping("/findUser/{name}")
    public List<GenericDataDTO> findUser(@PathVariable String name) {
        return userService.findUsers(name);
    }
}
