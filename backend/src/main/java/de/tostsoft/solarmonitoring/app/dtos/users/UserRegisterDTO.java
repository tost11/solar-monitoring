package de.tostsoft.solarmonitoring.app.dtos.users;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {

  @NotNull
  private String name;
  @NotNull
  private String password;
}
