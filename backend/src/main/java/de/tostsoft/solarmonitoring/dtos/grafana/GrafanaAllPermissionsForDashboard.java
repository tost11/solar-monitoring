package de.tostsoft.solarmonitoring.dtos.grafana;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class GrafanaAllPermissionsForDashboard {
    private long id;
    private long dashboardId;
    private Date created;
    private Date updated;
    private long userId;
    private String userLogin;
    private String userEmail;
    private long teamId;
    private String team;
    private String role;
    private long permission;
    private String permissionName;
    private String uid;
    private String title;
    private String slug;
    private Boolean isFolder;
    private String url;
}
