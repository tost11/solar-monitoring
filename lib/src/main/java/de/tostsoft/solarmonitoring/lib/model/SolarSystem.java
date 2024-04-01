package de.tostsoft.solarmonitoring.lib.model;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


import java.time.Duration;
import java.time.Instant;
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

  @NotNull
  private String name;

  @NotNull
  private String viewName;

  @Indexed(unique = true,sparse = true)
  private String shortener;

  private String token;

  private boolean needsStatisticRecalculation;

  @NotNull
  private LocalDateTime creationDate;
  private LocalDateTime buildingDate;
  private SolarSystemType type;

  private Double latitude;

  private Double longitude;

  @NotNull
  private ViewData viewData;

  @NotNull
  private TotalValues totalValues;

  private CurrentValues currentValues;

  private PublicMode publicMode;

  private String timezone;

  private Float electricityPrice;

  private Long lastCalculation;
  private Long lastManualCalculation;

  private Map<Integer,DeviceNamings> namings;

  private Set<Long> deyeSunSerials;

  @NotNull
  @Indexed(unique=true)
  private String influxTagName;

  @DocumentReference(lazy = true)
  @NotNull
  private User ownedBy;

  @DocumentReference(lazy = false, lookup = "{ 'solarSystem' : ?#{#self._id} }")
  @ReadOnlyProperty
  private List<Manages> managedBy;

  @NotNull
  private LocalDateTime deletedAt;

  @DocumentReference(lazy = false, lookup = "{ 'solarSystem' : ?#{#self._id} }")
  private List<Notification> notifier;

  public List<Manages> getManagedBy() {
    return managedBy.stream().filter(m->m.getUser().getDeletedAt() == null).collect(Collectors.toList());
  }

  public ZonedDateTime getCreationDateZoned(){
    var zone = timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getTimeZone("UTC");
    return creationDate.atZone(zone.toZoneId());
  }

  public boolean isOnline(){
    if(currentValues == null){
      return false;
    }
    return currentValues.isUpToDate(viewData != null ? viewData.getDefaultDelay():null);
  }
}
