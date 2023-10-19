package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;

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
