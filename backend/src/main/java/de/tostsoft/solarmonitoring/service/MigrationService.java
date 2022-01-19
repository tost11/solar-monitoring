package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.SolarSystemDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationService {

    @Autowired
    private GrafanaService grafanaService;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    public void migrate(SolarSystemType type) {
        List<SolarSystem> systems = solarSystemRepository.findAllByType(type);
        systems.forEach(system -> {
            SolarSystemDTO solarSystemDTO = new SolarSystemDTO();
            solarSystemDTO.setToken(system.getToken());
            solarSystemDTO.setType(system.getType());
            solarSystemDTO.setName(system.getName());
            grafanaService.createNewSelfmadeDeviceSolarDashboard("generated " + system.getRelationOwnedBy().getName(), solarSystemDTO, system.getRelationOwnedBy().getGrafanaFolderUid(), system.getGrafanaUid());
        });
    }

}
