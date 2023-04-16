package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.Converter;
import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManagerService {
    /*@Autowired
    private Neo4jUserRepository neo4jUserRepository;
    @Autowired
    private Neo4jSolarSystemRepository neo4jSolarSystemRepository;
    @Autowired
    private SolarSystemService solarSystemService;*/

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagesRepository managesRepository;

    public SolarSystemDTO addOrUpdateManageUser(SolarSystem solarSystem, AddManagerDTO addManagerDTO, User manager) {
        for (Manages manages : solarSystem.getManagedBy()) {
            if(manages.getUser().equals(manager)){
                manages.setPermission(addManagerDTO.getRole());
                managesRepository.save(manages);
                return Converter.convertSystemToDTO(solarSystem,true);
            }
        }

        var manages = Manages.builder()
            .user(manager)
            .solarSystem(solarSystem)
            .permission(addManagerDTO.getRole())
            .build();

        manages = managesRepository.save(manages);
        solarSystem.getManagedBy().add(manages);

        return Converter.convertSystemToDTO(solarSystem,true);
    }

    public SolarSystemDTO deleteManager(SolarSystem system, String managerId) {
        for (Manages manages : system.getManagedBy()) {
            if(StringUtils.equals(manages.getUser().getId(),managerId)){
                managesRepository.delete(manages);
                system.getManagedBy().remove(manages);
                break;
            }
        }

        return Converter.convertSystemToDTO(system, true);
    }
}
