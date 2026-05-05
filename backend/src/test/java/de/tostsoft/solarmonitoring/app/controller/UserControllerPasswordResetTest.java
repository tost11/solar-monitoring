package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.users.PasswordResetConfirmDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.PasswordResetRequestDTO;
import de.tostsoft.solarmonitoring.app.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserControllerPasswordResetTest extends AppBaseTest {

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private de.tostsoft.solarmonitoring.lib.service.MailService mailService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        when(mailService.isMailConfigured()).thenReturn(true);
    }

    @Test
    public void passwordResetRequest_validRequest_returnsSuccess() throws Exception {
        doNothing().when(passwordResetService).requestPasswordReset(
            anyString(), anyString(), anyString(), anyString()
        );

        PasswordResetRequestDTO dto = new PasswordResetRequestDTO();
        dto.setUsernameOrEmail("testuser");
        dto.setCaptchaImage("captcha-image");
        dto.setCaptchaText("12345");

        var response = doRestRequest("/api/user/password-reset/request", dto, HttpMethod.POST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordResetService).requestPasswordReset(
            eq("testuser"),
            eq("captcha-image"),
            eq("12345"),
            anyString()
        );
    }

    @Test
    public void validateToken_validToken_returnsTrue() throws Exception {
        when(passwordResetService.validateResetToken("valid-token")).thenReturn(true);

        var response = doRestRequest("/api/user/password-reset/validate/valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(result.get("valid")).isEqualTo(true);
    }

    @Test
    public void validateToken_invalidToken_returnsFalse() throws Exception {
        when(passwordResetService.validateResetToken("invalid-token")).thenReturn(false);

        var response = doRestRequest("/api/user/password-reset/validate/invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(result.get("valid")).isEqualTo(false);
    }

    @Test
    public void confirmPasswordReset_validRequest_returnsSuccess() throws Exception {
        doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

        PasswordResetConfirmDTO dto = new PasswordResetConfirmDTO();
        dto.setToken("valid-token");
        dto.setNewPassword("NewPassword123!");

        var response = doRestRequest("/api/user/password-reset/confirm", dto, HttpMethod.POST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(result.get("message")).isEqualTo("Password reset successful. You can now log in with your new password.");

        verify(passwordResetService).resetPassword("valid-token", "NewPassword123!");
    }

    @Test
    public void passwordResetEndpoints_publicAccess_noAuthenticationRequired() throws Exception {
        when(passwordResetService.validateResetToken("test-token")).thenReturn(true);

        var response = doRestRequest("/api/user/password-reset/validate/test-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void passwordResetRequest_mailNotConfigured_returnsInternalServerError() throws Exception {
        when(mailService.isMailConfigured()).thenReturn(false);

        PasswordResetRequestDTO dto = new PasswordResetRequestDTO();
        dto.setUsernameOrEmail("testuser");
        dto.setCaptchaImage("captcha-image");
        dto.setCaptchaText("12345");

        try {
            doRestRequest("/api/user/password-reset/request", dto, HttpMethod.POST);
            throw new AssertionError("Expected InternalServerError");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("500");
            assertThat(e.getMessage()).contains("not available");
        }

        verify(passwordResetService, never()).requestPasswordReset(anyString(), anyString(), anyString(), anyString());
    }
}
