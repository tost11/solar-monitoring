package de.tostsoft.solarmonitoring.app.dtos.users;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {

  //size values only for double check of length

  @NotNull
  @Size(max=255)
  private String name;
  @NotNull
  @Size(max=255)
  private String password;
  @NotNull
  @Size(max=10000)
  private String captcha;
  @NotNull
  @Size(max=255)
  private String captchaText;
  @NotNull
  @Size(max=300)
  private String mail;
}
