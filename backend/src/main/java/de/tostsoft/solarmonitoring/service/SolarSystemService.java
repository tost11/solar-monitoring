package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class SolarSystemService{

    @Autowired
    private UserService userService;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private UserRepository userRepository;


    public SolarSystem add (SolarSystemDTO solarSystemDTO){
        Date creationDate = new Date((long)solarSystemDTO.getCreationDate()*1000);
        solarSystemDTO.setToken(UUID.randomUUID().toString());

        if(solarSystemDTO.getLatitude()!=null && solarSystemDTO.getLongitude()!=null){
            SolarSystem solarSystem= new SolarSystem(solarSystemDTO.getToken(),solarSystemDTO.getName(),creationDate,solarSystemDTO.getType());
            solarSystem.setLatitude(solarSystemDTO.getLatitude());
            solarSystem.setLongitude(solarSystemDTO.getLongitude());
        }
        SolarSystem solarSystem= new SolarSystem(solarSystemDTO.getToken(),solarSystemDTO.getName(),creationDate,solarSystemDTO.getType());
        solarSystemRepository.save(solarSystem);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.addMySystems(solarSystem);
        userRepository.save(user);
        return solarSystem;
    }

    public ResponseEntity allwaysexist(){

        return ResponseEntity.status(HttpStatus.OK).body("");

    }
    public SolarSystem getSystem(String token){
        SolarSystem solarSystem = solarSystemRepository.findByToken(token);
        return solarSystem;

    }
    public User getUserBySystemToken(String token){
        SolarSystem solarSystem = solarSystemRepository.findByToken(token);
        User userOwn =solarSystem.getRelationOwns();
        return userOwn;

    }

    public void deleteSystem(String token) throws Exception {
       User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
       SolarSystem solarSystem =solarSystemRepository.findByToken(token);
       if(solarSystem==null){
           throw new Exception("System not exist");

    }
       if(user.getRelationOwns().contains(solarSystem)){
           solarSystemRepository.deleteByToken(token);
       }
    }
}
