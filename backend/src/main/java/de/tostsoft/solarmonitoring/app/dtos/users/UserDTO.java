package de.tostsoft.solarmonitoring.app.dtos.users;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserDTO {
    @NonNull
    @NotNull
    private String id;
    @NonNull
    @NotNull
    private String name;

    private String mail;

    private String jwt;

    private boolean isAdmin;

    private int numAllowedSystems;

    private List<NotificationDTO> notifications;

    private List<UserAccessSystemDTO> accessSystems;
}

