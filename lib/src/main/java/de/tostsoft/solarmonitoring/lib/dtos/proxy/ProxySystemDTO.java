package de.tostsoft.solarmonitoring.lib.dtos.proxy;

import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProxySystemDTO {
  @NonNull
  private String id;

  @NonNull
  private String token;

  private Set<Long> deyeSunSerials;

  private List<AccessToken> tokens;
}
