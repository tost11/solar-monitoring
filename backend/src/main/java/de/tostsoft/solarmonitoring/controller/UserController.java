package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.exception.InternalServerException;
import de.tostsoft.solarmonitoring.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO userLoginDTO) {

        ResponseEntity authenticationResponse = userService.loginMachCheck(userLoginDTO);
        UserDTO userDTO = new UserDTO(userLoginDTO.getName());
        userDTO.setJwt((String) authenticationResponse.getBody());

        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PostMapping("/register")
    public ResponseEntity registerUser(@RequestBody UserLoginDTO userLoginDTO) {
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

        if (requestIsValid == false) {
            LOG.error("User not createt :{}", responseMessage);
            throw new InternalServerException("responseMessage");
        }
        UserDTO userDTO;
        try {
            ResponseEntity authenticationResponse = userService.userRegister(userLoginDTO);
            userDTO = new UserDTO(userLoginDTO.getName());
            userDTO.setJwt((String) authenticationResponse.getBody());
            LOG.info("User created : {}", userLoginDTO.getName());
        } catch (Exception e) {
            throw new InternalServerException("Save fail");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }
}
