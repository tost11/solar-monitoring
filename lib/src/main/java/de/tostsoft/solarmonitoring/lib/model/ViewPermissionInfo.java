package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewPermissionInfo {
    private PublicMode permission;
    private String linkId;
}
