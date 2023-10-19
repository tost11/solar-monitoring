package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class UserRegisterDTO {

  @NonNull
  private String name;
  @NonNull
  private String password;
}
