package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.Manages;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerService {
    /*@Autowired
    private Neo4jUserRepository neo4jUserRepository;
    @Autowired
    private Neo4jSolarSystemRepository neo4jSolarSystemRepository;
    @Autowired
    private SolarSystemService solarSystemService;*/

    @Autowired
    private SolarSystemService solarSystemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagesRepository managesRepository;

    public SolarSystemDTO addOrUpdateManageUser(SolarSystem solarSystem, AddManagerDTO addManagerDTO) {
        var managerOpt = userRepository.findById(addManagerDTO.getId());
        if(managerOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        var manager = managerOpt.get();
        for (Manages manages : solarSystem.getManagedBy()) {
            if(manages.getUser().equals(manager)){
                manages.setPermission(addManagerDTO.getRole());
                managesRepository.save(manages);
                return solarSystemService.convertSystemToDTO(solarSystem,true);
            }
        }

        var manages = Manages.builder()
            .user(manager)
            .solarSystem(solarSystem)
            .build();

        manages = managesRepository.save(manages);
        solarSystem.getManagedBy().add(manages);

        return solarSystemService.convertSystemToDTO(solarSystem,true);
    }

    public ManagerDTO convertManagesToManagerDTO(Manages manages) {
        return new ManagerDTO(manages.getUser().getId(), manages.getUser().getViewName(), manages.getPermission());
    }

    public List<ManagerDTO> convertListManagesToManagerDTO(List<Manages> manages) {
        return manages.stream().map(this::convertManagesToManagerDTO).collect(Collectors.toList());
    }

    public SolarSystemDTO deleteManager(SolarSystem system, String managerId) {
        for (Manages manages : system.getManagedBy()) {
            if(StringUtils.equals(manages.getUser().getId(),managerId)){
                managesRepository.delete(manages);
                system.getManagedBy().remove(manages);
                break;
            }
        }

        return solarSystemService.convertSystemToDTO(system, true);
    }
}
