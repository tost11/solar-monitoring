package de.tostsoft.solarmonitoring.app.dtos.users;

import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CreatePatchNotificationDTO {
    @NonNull
    private String id;

    @NonNull
    private NotificationType type;
    @NonNull
    private String value;

    @NonNull
    private String solarSystemId;
}
