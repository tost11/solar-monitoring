package de.tostsoft.solarmonitoring.app.dtos.users;

import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class NotificationDTO {
    @NotNull
    private String id;

    @NotNull
    private NotificationType type;
    private String value;

    @NotNull
    private String solarSystemId;
    @NotNull
    private String solarSystemName;
    @NotNull
    private SolarSystemType solarSystemType;
}
