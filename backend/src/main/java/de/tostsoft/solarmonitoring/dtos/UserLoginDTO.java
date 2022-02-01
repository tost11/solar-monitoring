package de.tostsoft.solarmonitoring.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class UserLoginDTO {

  @NonNull
  private String name;

  @NonNull
  private String password;


}
