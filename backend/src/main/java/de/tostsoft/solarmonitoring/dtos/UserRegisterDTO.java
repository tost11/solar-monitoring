package de.tostsoft.solarmonitoring.dtos;

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