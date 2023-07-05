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
  private HashMap<Long,String> devices;
  private HashMap<Long,String> inputs;
  private HashMap<Long,String> outputs;
  private HashMap<Long,String> batteries;
}
