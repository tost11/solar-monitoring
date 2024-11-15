package de.tostsoft.solarmonitoring.whatever.model;

import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySampleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class ProxySolarSample {

  @Id
  private String id;

  @NonNull
  private ProxySampleDTO sample;
}
