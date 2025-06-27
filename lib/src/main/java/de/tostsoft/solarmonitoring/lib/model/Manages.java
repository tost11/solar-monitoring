package de.tostsoft.solarmonitoring.lib.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Manages {

  @Id
  private String id;

  @NotNull
  @DocumentReference(lazy = true)
  private SolarSystem solarSystem;

  @NotNull
  private Permissions permission;

  @NotNull
  @DocumentReference(lazy = true)
  private User user;
}
