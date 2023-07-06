package de.tostsoft.solarmonitoring.model;

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
public class Namings {
  private HashMap<String,String> devices;
  private HashMap<String,String> inputs;
  private HashMap<String,String> outputs;
  private HashMap<String,String> batteries;
}
