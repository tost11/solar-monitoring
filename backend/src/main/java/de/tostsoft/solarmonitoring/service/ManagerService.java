package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.ManageBY;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ManagerService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private SolarSystemService solarSystemService;

    public SolarSystemDTO addManageUser(SolarSystem solarSystem, AddManagerDTO addManagerDTO) {
        User manager = userRepository.findById(addManagerDTO.getId());
        if(manager == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        for (ManageBY manageBY : solarSystem.getRelationManageBy()) {
            if(manageBY.getUser().getId().equals(manager.getId())){
                if(manageBY.getPermission() == addManagerDTO.getRole()) {//everything is fine already right
                    return solarSystemService.convertSystemToDTO(solarSystem,true);
                }
                manageBY.setPermission(addManagerDTO.getRole());
                solarSystemRepository.updateManageRelation(manageBY.getId(),""+addManagerDTO.getRole());
                return solarSystemService.convertSystemToDTO(solarSystem,true);
            }
        }
        Long id = solarSystemRepository.addManageRelation(addManagerDTO.getId(),solarSystem.getId(),""+addManagerDTO.getRole());
        var newManage = new ManageBY(id,manager,addManagerDTO.getRole());
        solarSystem.getRelationManageBy().add(newManage);
        return solarSystemService.convertSystemToDTO(solarSystem,true);
    }

    public List<ManagerDTO> getManagers(SolarSystem system) {
        ArrayList<ManagerDTO> managers=new ArrayList<>();
        for(ManageBY manageBy: system.getRelationManageBy()){
            managers.add(new ManagerDTO(manageBy.getUser().getId(),manageBy.getUser().getName(),manageBy.getPermission()));
        }
        return managers;
    }

    public SolarSystemDTO deleteManager(SolarSystem system, long managerId) {
        ManageBY manager = system.getRelationManageBy().stream().filter(m->m.getUser().getId().equals(managerId)).findAny().orElse(null);
        if(manager==null) {
            //If manager is not there everything is okay response actual ManagerList
            return solarSystemService.convertSystemToDTO(system, true);
        }
        solarSystemRepository.deleteManagerRelation(manager.getId());
        system.getRelationManageBy().remove(manager);
        return solarSystemService.convertSystemToDTO(system, true);
    }
}
