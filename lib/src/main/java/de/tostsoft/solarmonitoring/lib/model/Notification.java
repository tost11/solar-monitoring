package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @NotNull
    @DocumentReference(lazy = true)
    private SolarSystem solarSystem;

    private NotificationType type;

    private String value;

    @NotNull
    @DocumentReference(lazy = true)
    private User user;
}
