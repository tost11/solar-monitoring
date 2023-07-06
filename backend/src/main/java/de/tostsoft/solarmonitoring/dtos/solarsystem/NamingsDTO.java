package de.tostsoft.solarmonitoring.dtos.solarsystem;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class NamingsDTO {
  @NotNull
  private HashMap<String,String> devices;
  @NotNull
  private HashMap<String,String> inputs;
  @NotNull
  private HashMap<String,String> outputs;
  @NotNull
  private HashMap<String,String> batteries;
}
