package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.Neo4jManageBy;
import de.tostsoft.solarmonitoring.model.Neo4jSolarSystem;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.repository.Neo4jSolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerService {
    @Autowired
    private Neo4jUserRepository neo4jUserRepository;
    @Autowired
    private Neo4jSolarSystemRepository neo4jSolarSystemRepository;
    @Autowired
    private SolarSystemService solarSystemService;

    public SolarSystemDTO addManageUser(Neo4jSolarSystem neo4jSolarSystem, AddManagerDTO addManagerDTO) {
        Neo4jUser manager = neo4jUserRepository.findById(addManagerDTO.getId());
        if(manager == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        for (Neo4jManageBy neo4jManageBy : neo4jSolarSystem.getRelationNeo4jManageBy()) {
            if(neo4jManageBy.getNeo4jUser().getId().equals(manager.getId())){
                if(neo4jManageBy.getPermission() == addManagerDTO.getRole()) {//everything is fine already right
                    return solarSystemService.convertSystemToDTO(neo4jSolarSystem,true);
                }
                neo4jManageBy.setPermission(addManagerDTO.getRole());
                neo4jSolarSystemRepository.updateManageRelation(neo4jManageBy.getId(),""+addManagerDTO.getRole());
                return solarSystemService.convertSystemToDTO(neo4jSolarSystem,true);
            }
        }
        Long id = neo4jSolarSystemRepository.addManageRelation(addManagerDTO.getId(), neo4jSolarSystem.getId(),""+addManagerDTO.getRole());
        var newManage = new Neo4jManageBy(id,manager,addManagerDTO.getRole());
        neo4jSolarSystem.getRelationNeo4jManageBy().add(newManage);
        return solarSystemService.convertSystemToDTO(neo4jSolarSystem,true);
    }

    public List<ManagerDTO> getManagers(Neo4jSolarSystem system) {
        ArrayList<ManagerDTO> managers=new ArrayList<>();
        for(Neo4jManageBy neo4jManageBy : system.getRelationNeo4jManageBy()){
            managers.add(new ManagerDTO(neo4jManageBy.getNeo4jUser().getId(), neo4jManageBy.getNeo4jUser().getName(),
                neo4jManageBy.getPermission()));
        }
        return managers;
    }

    public SolarSystemDTO deleteManager(Neo4jSolarSystem system, long managerId) {
        Neo4jManageBy manager = system.getRelationNeo4jManageBy().stream().filter(m->m.getNeo4jUser().getId().equals(managerId)).findAny().orElse(null);
        if(manager==null) {
            //If manager is not there everything is okay response actual ManagerList
            return solarSystemService.convertSystemToDTO(system, true);
        }
        neo4jSolarSystemRepository.deleteManagerRelation(manager.getId());
        system.getRelationNeo4jManageBy().remove(manager);
        return solarSystemService.convertSystemToDTO(system, true);
    }
}
