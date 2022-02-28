package de.tostsoft.solarmonitoring.dtos.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserTableRowForAdminDTO {
    private long Id;
    private String name;

    private int numbAllowedSystems;

    private boolean isAdmin;
}
