package de.tostsoft.solarmonitoring.model;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

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

  private String token;

  @NotNull
  private LocalDateTime creationDate;
  private LocalDateTime buildingDate;
  private SolarSystemType type;

  private Double latitude;

  private Double longitude;

  @NotNull
  private ViewData viewData;

  private PublicMode publicMode;

  private String timezone;

  private Long lastCalculation;
  private Long lastManualCalculation;

  private Namings namings;

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

  public List<Manages> getManagedBy() {
    return managedBy.stream().filter(m->m.getUser().getDeletedAt() == null).collect(Collectors.toList());
  }
}
