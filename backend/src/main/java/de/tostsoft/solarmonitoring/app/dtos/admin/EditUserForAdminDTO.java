package de.tostsoft.solarmonitoring.app.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EditUserForAdminDTO {
    @NotNull
    private String id;
    @NotNull
    private String name;

    private boolean isAdmin;

    private int numAllowedSystems;

    private boolean isDeleted;

    private String mail;
}

