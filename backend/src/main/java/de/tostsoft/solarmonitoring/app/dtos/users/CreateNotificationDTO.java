package de.tostsoft.solarmonitoring.app.dtos.users;

import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CreateNotificationDTO {
    @NonNull
    private String id;

    @NonNull
    private NotificationType type;

    private String value;
}
