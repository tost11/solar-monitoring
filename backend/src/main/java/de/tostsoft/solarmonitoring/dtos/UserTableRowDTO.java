package de.tostsoft.solarmonitoring.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class UserTableRowDTO {
    private long Id;
    private String name;

    private int numbAllowedSystems;

    private boolean isAdmin;


}
