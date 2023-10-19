package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {

  @Id
  private String id;

  @Indexed(unique = true)
  private String name;

  private Boolean isRegistrationEnabled;

  @Override
  public String toString() {
    return "Config{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", isRegistrationEnabled=" + isRegistrationEnabled +
        '}';
  }
}
