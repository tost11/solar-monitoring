package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class MigrationService implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);

    @Autowired
    private GrafanaService grafanaService;

    @Autowired
    private SolarSystemRepository solarSystemRepository;


    @Override
    public void run(String... args) {
        for (String arg : args) {
            if (arg.equals("migration")) {
                LOG.info("running mingration");
                migrate(SolarSystemType.SELFMADE);
                migrate(SolarSystemType.SELFMADE_DEVICE);
                migrate(SolarSystemType.SELFMADE_INVERTER);
                migrate(SolarSystemType.SELFMADE_CONSUMPTION);
            }
        }
    }

    public void migrate(SolarSystemType type) {
        if(type == SolarSystemType.SELFMADE || type == SolarSystemType.SELFMADE_DEVICE || type == SolarSystemType.SELFMADE_INVERTER || type == SolarSystemType.SELFMADE_CONSUMPTION) {
            List<SolarSystem> systems = solarSystemRepository.findAllByType(type);
            systems.forEach(system -> {
                RegisterSolarSystemResponseDTO solarSystemDTO = new RegisterSolarSystemResponseDTO();
                solarSystemDTO.setToken(system.getToken());
                solarSystemDTO.setType(system.getType());
                solarSystemDTO.setName(system.getName());
                grafanaService.createNewSelfmadeDeviceSolarDashboard(system);
            });
        }
    }

}
