package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class SolarSystem {
  @Id
  private String id;

  @Indexed(unique = true,sparse = true)
  private String shortener;

  private String token;

  private boolean needsStatisticRecalculation;

  @NotNull
  private LocalDateTime creationDate;

  private SolarSystemType type;

  @NotNull
  private ViewData viewData;

  @NotNull
  private TotalValues totalValues;

  private CurrentValues currentValues;

  @NotNull
  @Builder.Default
  private SystemInformations systemInformations = new SystemInformations();

  private PublicMode publicMode;

  private String timezone;

  private Long maxSamplesOnDay;

  private Long lastCalculation;
  private Long lastManualCalculation;
  private Boolean lastOnlineCheckStatus;

  private Boolean calculateCombinedValuesAfterwards;

  private Map<Long,DeviceNamings> namings;

  private Set<Long> deyeSunSerials;

  @Builder.Default
  private List<AccessToken> tokens = new ArrayList<>();

  @NotNull
  @Indexed(unique=true)
  private String influxTagName;

  @DocumentReference(lazy = true)
  @NotNull
  private User ownedBy;

  @DocumentReference(lazy = false, lookup = "{ 'solarSystem' : ?#{#self._id} }")
  @ReadOnlyProperty
  private List<Manages> managedBy;

  private LocalDateTime deletedAt;

  @DocumentReference(lazy = false, lookup = "{ 'solarSystem' : ?#{#self._id} }")
  private List<Notification> notifier;

  @Indexed
  @DocumentReference(lazy = true)
  private List<Tag> tags;

  public List<Manages> getManagedBy() {
    return managedBy.stream().filter(m->m.getDeletedAt() == null && m.getUser().getDeletedAt() == null).collect(Collectors.toList());
  }

  public ZonedDateTime getCreationDateZoned(){
    var zone = timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getTimeZone("UTC");
    return creationDate.atZone(zone.toZoneId());
  }

  public boolean isOnline(){
    if(viewData != null && viewData.getDefaultDelay() != null){
      return isOnline(Duration.ofSeconds(viewData.getDefaultDelay()));
    }
    return isOnline(null);
  }

  public boolean isOnline(Duration timeout){
    if(currentValues == null){
      return false;
    }
    return currentValues.isUpToDate(timeout);
  }

}
