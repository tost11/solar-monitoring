package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.Response;
import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.module.SolarSystem;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Service
public class SolarSystemService{
    @Autowired
    private UserService userService;
    @Autowired
    private SolarSystemRepository solarSystemRepository;


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
        return solarSystem;
    }
    public ResponseEntity allwaysexist(){

        return ResponseEntity.status(HttpStatus.OK).body("");

    }
}
