package de.tostsoft.solarmonitoring.app.dtos;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenericDataDTO {
  @NotNull
  private String id;
  @NotNull
  private String name;
}
