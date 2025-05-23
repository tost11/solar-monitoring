package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterInfoDTO {
    private String captcha;
}
