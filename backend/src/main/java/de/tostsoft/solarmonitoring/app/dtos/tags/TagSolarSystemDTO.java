package de.tostsoft.solarmonitoring.app.dtos.tags;

import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TagSolarSystemDTO {

    //TODO little graph vor visualisation

    TagDTO tag;

    List<SolarSystemListItemDTO> systems;
}
