package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.Data;

@Data
public class PasswordResetRequestDTO {
    private String usernameOrEmail;
    private String captchaImage;
    private String captchaText;
}
