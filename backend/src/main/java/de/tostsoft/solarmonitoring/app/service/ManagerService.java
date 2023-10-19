package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.dtos.AddManagerDTO;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.ManagesRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagerService {

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
