package de.tostsoft.solarmonitoring.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
