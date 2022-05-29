package de.tostsoft.solarmonitoring.dtos.users;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
