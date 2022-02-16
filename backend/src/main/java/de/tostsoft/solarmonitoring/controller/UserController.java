package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.AdminDTO;
import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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
        var jwt = userService.loginUser(userLoginDTO);
        UserDTO userDTO = new UserDTO(userLoginDTO.getName());
        userDTO.setJwt(jwt);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    //TODO restrigt input of username to normal characters number and spaces
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserRegisterDTO userRegisterDTO) {
        boolean requestIsValid = true;
        String responseMessage = "";

        userRegisterDTO.setName(StringUtils.trim(userRegisterDTO.getName()));

        if (StringUtils.length(userRegisterDTO.getName()) < 4) {
            requestIsValid = false;
            responseMessage += "\n Username must contain at least 4 characters";
        } else {
            if (userService.isUserAlreadyExists(userRegisterDTO)) {
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
    @PostMapping("/toAdmin/{name}")
    public ResponseEntity<UserDTO> makeUserToAdmin(@PathVariable String name) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.isAdmin()) {
          return userService.makeUserToAdmin(name);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not a Admin");
    }

    @GetMapping("/isUser/Admin")
    private AdminDTO isUserAdmin(){
        User user= (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.isAdmin()) {
            return new AdminDTO(user.getName(),user.isAdmin());
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"You Are not a Admin");
    }


}
