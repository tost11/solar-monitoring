package de.tostsoft.solarmonitoring.app.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTableRowForAdminDTO {
    @NotNull
    private String Id;

    private String name;

    private int numAllowedSystems;

    private boolean isAdmin;

    private boolean isDeleted;
}
