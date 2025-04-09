package de.tostsoft.solarmonitoring.app.dtos.users;

import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CreateNotificationDTO {
    @NotNull
    private String id;

    @NotNull
    private NotificationType type;

    private String value;
}
