package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.ManageBY;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
@Service
public class ManagerService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private SolarSystemService solarSystemService;

    public SolarSystemDTO addManageUser(SolarSystem solarSystem, AddManagerDTO addManagerDTO) {

        //TODO refactor to return ManagerList or stay by system ?
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
                //TODO refactor to only save this relation (besides here is a bug deleted users will be lose their relations"
                return solarSystemService.convertSystemToDTO(solarSystemRepository.save(solarSystem),true);
            }
        }
        grafanaService.setPermissionsForDashboard(solarSystem.getRelationManageBy(),manager.getGrafanaUserId(),solarSystem.getGrafanaId());
        solarSystem.getRelationManageBy().add(new ManageBY(manager,addManagerDTO.getRole()));
        solarSystem = solarSystemRepository.save(solarSystem);
        System.out.println(manager.getGrafanaUserId());
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
        system.getRelationManageBy().remove(manager);
        system = solarSystemRepository.save(system);
        grafanaService.setPermissionsForDashboard(system.getRelationManageBy(),null,system.getGrafanaId());
        return solarSystemService.convertSystemToDTO(system, true);
    }
}
