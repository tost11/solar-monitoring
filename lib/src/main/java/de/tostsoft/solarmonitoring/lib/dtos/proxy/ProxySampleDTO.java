package de.tostsoft.solarmonitoring.lib.dtos.proxy;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxySampleDTO {

  @NonNull
  private String systemId;

  @NonNull
  private SampleDTO sampleDTO;
}
