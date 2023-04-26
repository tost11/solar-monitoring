package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.Converter;
import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.List;
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

    public List<Manages> addOrUpdateManageUser(SolarSystem solarSystem, AddManagerDTO addManagerDTO, User manager) {
        var managesList = solarSystem.getManagedBy();
        for (Manages manages : managesList) {
            if(manages.getUser().equals(manager)){
                manages.setPermission(addManagerDTO.getRole());
                managesRepository.save(manages);
                return managesList;
            }
        }

        var manages = Manages.builder()
            .user(manager)
            .solarSystem(solarSystem)
            .permission(addManagerDTO.getRole())
            .build();

        manages = managesRepository.save(manages);
        managesList.add(manages);

        return managesList;
    }

    public List<Manages> deleteManager(SolarSystem system, String managerId) {
        var managesList = system.getManagedBy();
        for (Manages manages :managesList) {
            if(StringUtils.equals(manages.getUser().getId(),managerId)){
                managesRepository.delete(manages);
                managesList.remove(manages);
                break;
            }
        }

        return managesList;
    }
}
