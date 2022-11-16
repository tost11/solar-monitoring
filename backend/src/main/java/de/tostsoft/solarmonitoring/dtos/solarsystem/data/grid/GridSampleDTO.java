package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseSampleDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GridSampleDTO extends BaseSampleDTO<GridDeviceDTO> {
}
