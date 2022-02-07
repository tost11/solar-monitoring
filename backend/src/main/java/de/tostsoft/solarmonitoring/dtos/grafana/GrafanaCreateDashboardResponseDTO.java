package de.tostsoft.solarmonitoring.dtos.grafana;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class GrafanaCreateDashboardResponseDTO {
  private long id;
  private String uid;
  private String url;
  private String status;
  private int version;
  private String slug;
}
