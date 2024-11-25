package de.tostsoft.solarmonitoring.app.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserForAdminDTO {
    @NotNull
    private String id;
    @NotNull
    private String name;

    private boolean isAdmin;

    private int numbAllowedSystems;

    private ZonedDateTime creationDate;

    private boolean isDeleted;
}

