package de.tostsoft.solarmonitoring.app.dtos.users;

import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserAccessSystemDTO {

    @NotNull
    String id;

    @NotNull
    String name;

    @NotNull
    SolarSystemType type;
}
