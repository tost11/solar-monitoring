package de.tostsoft.solarmonitoring.dtos.grafana;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrafanaDashboardDTO {
       private long id;
       private String uid;
       private String title;
       private String uri;
       private String url;
       private String slug;
       private String type;
       private String[] tags;
       private boolean isStarred;
       private long folderId;
       private String folderUid;
       private String folderTitle;
       private String folderUrl;
       private long sortMeta;
}
