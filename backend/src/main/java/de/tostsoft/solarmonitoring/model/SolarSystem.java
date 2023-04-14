package de.tostsoft.solarmonitoring.model;

import de.tostsoft.solarmonitoring.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Relationship;

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

  private String token;

  @NotNull
  private ZonedDateTime creationDate;
  private ZonedDateTime buildingDate;
  private SolarSystemType type;

  private boolean isDeleted;

  private Double latitude;

  private Double longitude;

  private Boolean isBatteryPercentage;
  private Boolean hasACInput;
  private Boolean hasDCOutput;
  private Boolean hasACOutput;
  private Boolean showAmpere;

  private Integer voltageAC;

  private Integer batteryVoltage;

  private Integer maxSolarVoltage;

  private PublicMode publicMode;

  private String timezone;

  private ZonedDateTime lastCalculation;
  private ZonedDateTime lastManualCalculation;

  @DocumentReference(lazy = true)
  @NotNull
  private User ownedBy;

  @DocumentReference(lazy = false, lookup = "{ 'solarSystem' : ?#{#self._id} }")
  @ReadOnlyProperty
  private List<Manages> managedBy;
}
