package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class UserDTO {
    @NonNull
    private String id;
    @NonNull
    private String name;

    private String jwt;

    private boolean isAdmin;

    private int numAllowedSystems;
}

