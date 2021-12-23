package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
        var jwt = userService.loginUser(userLoginDTO);
        UserDTO userDTO = new UserDTO(userLoginDTO.getName());
        userDTO.setJwt(jwt);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PostMapping("/login/token")
    public ResponseEntity<UserDTO> login(@RequestBody String token) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDTO userDTO = new UserDTO(user.getName());
        userDTO.setJwt(token);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserLoginDTO userLoginDTO) {
        boolean requestIsValid = true;
        String responseMessage = "";

        if (StringUtils.length(userLoginDTO.getName()) < 4) {
            requestIsValid = false;
            responseMessage += "\n Username must contain at least 4 characters";
        } else {
            if (userService.isUserAlreadyExists(userLoginDTO)) {
                LOG.error("User is allredy used");
                requestIsValid = false;
                responseMessage += "\n Username is already taken";
            }
        }
        if (StringUtils.isEmpty(userLoginDTO.getPassword())) {
            requestIsValid = false;
            responseMessage += "\n No password has been entered";

        } else if (userLoginDTO.getPassword().length() < 8) {
            requestIsValid = false;
            responseMessage += "\n Password must contain at least 8 characters";
        }

        if (!requestIsValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, responseMessage);
        }
        var jwt = userService.registerUser(userLoginDTO);
        var userDTO = new UserDTO(userLoginDTO.getName());
        userDTO.setJwt(jwt);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }
}
