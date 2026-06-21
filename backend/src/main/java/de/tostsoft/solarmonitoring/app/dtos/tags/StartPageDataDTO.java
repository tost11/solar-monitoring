package de.tostsoft.solarmonitoring.app.dtos.tags;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StartPageDataDTO {
    private List<TagSolarSystemDTO> tagsWithSystems;
    private List<TagDTO> aggregationTags;
}
