package de.tostsoft.solarmonitoring.app.dtos.users;

import lombok.Data;

@Data
public class PasswordResetConfirmDTO {
    private String token;
    private String newPassword;
}
