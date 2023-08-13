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
public class DeviceNamings {

  private String name;

  private HashMap<Integer,String> inputsDC;
  private HashMap<Integer,String> inputsAC;
  private HashMap<Integer,String> outputsDC;
  private HashMap<Integer,String> outputsAC;
  private HashMap<Integer,String> batteries;

  public DeviceNamings(String name) {
    this.name = name;
    inputsDC = new HashMap<>();
    inputsAC = new HashMap<>();
    outputsDC = new HashMap<>();
    outputsAC = new HashMap<>();
    batteries = new HashMap<>();
  }
}
