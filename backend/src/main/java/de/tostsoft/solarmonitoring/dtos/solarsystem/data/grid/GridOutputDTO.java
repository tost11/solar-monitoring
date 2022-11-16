package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseOutputDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GridOutputDTO extends BaseOutputDTO {
  private Integer phase;
}
