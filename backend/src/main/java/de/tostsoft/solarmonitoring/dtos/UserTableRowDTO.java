package de.tostsoft.solarmonitoring.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class UserTableRowDTO {
    private long id;
    private String name;

    private int numAllowedSystems;

    private boolean isAdmin;


}
