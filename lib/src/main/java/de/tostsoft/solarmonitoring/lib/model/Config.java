package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Config {

  @Id
  private String id;

  @Indexed(unique = true)
  private String name;

  private Boolean isRegistrationEnabled;

  private Integer dailyRegistrations;

  @Override
  public String toString() {
    return "Config{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", isRegistrationEnabled=" + isRegistrationEnabled +
            ", dailyRegistrations=" + dailyRegistrations +
            '}';
  }
}
