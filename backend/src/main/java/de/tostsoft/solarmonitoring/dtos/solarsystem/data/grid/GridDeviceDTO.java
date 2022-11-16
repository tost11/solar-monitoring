package de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.BaseInputDTO;
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
public class GridDeviceDTO extends BaseDeviceDTO<BaseInputDTO,GridOutputDTO> {
}
