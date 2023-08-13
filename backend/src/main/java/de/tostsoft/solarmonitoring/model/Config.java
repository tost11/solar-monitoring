package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
