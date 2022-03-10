package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.Permissions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AddManagerDTO {
    private long id;
    private long systemId;
    private Permissions role;

}

