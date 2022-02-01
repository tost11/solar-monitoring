package de.tostsoft.solarmonitoring.dtos.grafana;

import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GrafanaFolderResponseDTO {
  private long id;
  private String uid;
  private String title;
  private String url;
  private boolean hasAcl;
  private boolean canSave;
  private boolean canEdit;
  private boolean canAdmin;
  private String createdBy;
  private Date created;
  private String updatedBy;
  private Date updated;
  long version;
}
