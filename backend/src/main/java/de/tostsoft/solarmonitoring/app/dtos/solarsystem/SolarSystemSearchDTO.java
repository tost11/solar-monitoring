package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SolarSystemSearchDTO {

    private String name;

    private SolarSystemType type;

    private List<String> tags;

    @JsonProperty("public")
    private Boolean isPublic;
}
