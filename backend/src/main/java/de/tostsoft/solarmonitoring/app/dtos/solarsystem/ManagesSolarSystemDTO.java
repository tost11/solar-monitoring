package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.status.AllStatusResponseDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.NotificationDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ManagesSolarSystemDTO extends ViewSolarSystemDTO{

    @NotNull
    private String name;

    private AllStatusResponseDTO status;

    private String deyeSunSerialNumbers;

    private Boolean calculateCombinedValuesAfterwards;
}
