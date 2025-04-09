package de.tostsoft.solarmonitoring.lib.dtos.proxy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProxySystemsDTO {

  private List<ProxySystemDTO> systems;
}
