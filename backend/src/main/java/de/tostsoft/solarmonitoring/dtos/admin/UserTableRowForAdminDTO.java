package de.tostsoft.solarmonitoring.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserTableRowForAdminDTO {
    @NotNull
    private String Id;

    private String name;

    private int numAllowedSystems;

    private boolean isAdmin;

    private boolean isDeleted;
}
