package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.status.AllStatusResponseDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ManagesSolarSystemDTO extends ViewSolarSystemDTO{

    private AllStatusResponseDTO status;

    private String deyeSunSerialNumbers;

    private Boolean calculateCombinedValuesAfterwards;
}
