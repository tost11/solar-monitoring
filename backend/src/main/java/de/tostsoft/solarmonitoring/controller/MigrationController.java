package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.MigrationDTO;
import de.tostsoft.solarmonitoring.service.MigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    @Autowired
    private MigrationService migrationService;

    @PostMapping()
    public String migrate(@RequestBody MigrationDTO migrationDTO) {
        migrationService.migrate(migrationDTO.getType());
        return "Ok";
    }

}
