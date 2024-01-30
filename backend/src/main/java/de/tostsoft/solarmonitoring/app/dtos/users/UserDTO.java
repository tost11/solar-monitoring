package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class UserDTO {
    @NonNull
    private String id;
    @NonNull
    private String name;

    private String jwt;

    private boolean isAdmin;

    private int numAllowedSystems;

    private List<NotificationDTO> notifications;

    private List<UserAccessSystemDTO> accessSystems;
}

