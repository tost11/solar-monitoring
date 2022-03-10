package de.tostsoft.solarmonitoring.dtos.users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserTableRowDTO {
    private long id;
    private String name;

    private int numAllowedSystems;

    private boolean isAdmin;



}
