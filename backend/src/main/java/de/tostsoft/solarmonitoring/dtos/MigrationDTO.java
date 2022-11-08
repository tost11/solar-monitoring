package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MigrationDTO {

    private SolarSystemType type;
}
