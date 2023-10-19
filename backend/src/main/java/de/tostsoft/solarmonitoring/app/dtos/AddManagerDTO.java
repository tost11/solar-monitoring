package de.tostsoft.solarmonitoring.app.dtos;

import de.tostsoft.solarmonitoring.lib.model.Permissions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AddManagerDTO {
    private String id;

    private String systemId;

    private Permissions role;
}

