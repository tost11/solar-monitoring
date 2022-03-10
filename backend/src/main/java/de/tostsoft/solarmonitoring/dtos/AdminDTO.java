package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminDTO {
    private long id;

    private String name;

    private boolean isAdmin;
}

