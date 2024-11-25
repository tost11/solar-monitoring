package de.tostsoft.solarmonitoring.app.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UpdateUserForAdminDTO {
    @NotNull
    private String id;
    @NotNull
    private String name;

    private boolean admin;

    private int numAllowedSystems;

    private boolean isDeleted;
}

