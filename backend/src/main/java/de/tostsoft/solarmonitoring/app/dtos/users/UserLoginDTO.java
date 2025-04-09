package de.tostsoft.solarmonitoring.app.dtos.users;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserLoginDTO {

  @NotNull
  private String name;

  @NotNull
  private String password;


}
