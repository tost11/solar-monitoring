package de.tostsoft.solarmonitoring.app.dtos.admin;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class UserForAdminDTO {
    @NonNull
    private String id;
    @NonNull
    private String name;

    private boolean isAdmin;

    private int numbAllowedSystems;

    private ZonedDateTime creationDate;

    private boolean isDeleted;
}

