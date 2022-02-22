package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.Permissions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
@Setter
@AllArgsConstructor
public class ManagerDTO {
    private long id;
    private String userName;
    private Permissions role;

}
