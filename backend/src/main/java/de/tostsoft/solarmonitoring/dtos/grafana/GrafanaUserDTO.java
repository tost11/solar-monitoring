package de.tostsoft.solarmonitoring.dtos.grafana;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GrafanaUserDTO {
  private long id;
  private String name;
  private String login;
  private String email;
  private boolean isAdmin;
  private boolean isDisabled;
  private Date lastSeenAt;
  private String lastSeenAtAge;
  private List<String> authLabels;
}
