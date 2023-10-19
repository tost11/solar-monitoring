package de.tostsoft.solarmonitoring.app.dtos;

import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MigrationDTO {

    private SolarSystemType type;
}
