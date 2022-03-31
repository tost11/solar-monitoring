package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.admin.ConfigDTO;
import de.tostsoft.solarmonitoring.service.ConfigService;
import de.tostsoft.solarmonitoring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
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
