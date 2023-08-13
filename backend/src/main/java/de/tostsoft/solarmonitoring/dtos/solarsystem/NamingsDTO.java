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
  private HashMap<String,String> inputsDC;
  @NotNull
  private HashMap<String,String> inputsAC;
  @NotNull
  private HashMap<String,String> outputsDC;
  @NotNull
  private HashMap<String,String> outputsAC;
  @NotNull
  private HashMap<String,String> batteries;
}
