package de.tostsoft.solarmonitoring.dtos.grafana;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@Getter
public class GrafanaUserDTO {
    private long id;
    private String name;
    private String login;
    private String email;
    private boolean isAdmin;
    private boolean isDisabled;
    private Date lastSeenAt;
    private String lastSeenAtAge;
    private String [] authLabels;
}
