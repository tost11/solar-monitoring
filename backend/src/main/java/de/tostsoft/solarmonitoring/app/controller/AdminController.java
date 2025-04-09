package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.dtos.admin.ConfigDTO;
import de.tostsoft.solarmonitoring.app.service.ConfigService;
import de.tostsoft.solarmonitoring.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminController {

  @Autowired
  private ConfigService configService;

  @Autowired
  private UserService userService;

  @PostMapping("/config/registration")
  public void changeRegistrationStatus(@RequestParam boolean enabled){
    if(!userService.isUserFromContextAdmin()){
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have not permission to do that!");
    }
    configService.setRegistrationEnabled(enabled);
  }

  @GetMapping("/config")
  public ConfigDTO getConfig(){
    if(!userService.isUserFromContextAdmin()){
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have not permission to do that!");
    }
    return new ConfigDTO(configService.isRegistrationEnabled());
  }

}
