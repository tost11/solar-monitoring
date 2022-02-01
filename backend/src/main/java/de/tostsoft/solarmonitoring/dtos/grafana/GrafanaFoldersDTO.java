package de.tostsoft.solarmonitoring.dtos.grafana;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class GrafanaFoldersDTO {
  private long id;
  private String uid;
  private String title;
}
