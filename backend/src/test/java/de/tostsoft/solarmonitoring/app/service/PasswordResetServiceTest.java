package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.model.PasswordResetToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.PasswordResetTokenRepository;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "tokenValidityMinutes", 60);
        ReflectionTestUtils.setField(passwordResetService, "fulldomain", "http://localhost:8050");
    }

    @Test
    void requestPasswordReset_validUser_createsTokenAndSendsEmail() {
        User user = createTestUser("testuser", "test@example.com");
        var captcha = createMockCaptcha();

        when(captchaService.getCaptchaByByBase64Image(anyString())).thenReturn(captcha);
        when(userService.findUserByUsernameOrEmail("testuser")).thenReturn(user);
        when(passwordResetTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("testuser", "captchaImage", "12345", "127.0.0.1");

        verify(passwordResetTokenRepository).save(argThat(token ->
            token.getUserId().equals(user.getId()) &&
            token.getToken() != null &&
            token.getExpiresAt().isAfter(Instant.now())
        ));
        verify(mailService).sendMail(
            eq("test@example.com"),
            eq("Password Reset Request - Solar Monitoring"),
            contains("http://localhost:8050/reset-password?token=")
        );
    }

    @Test
    void requestPasswordReset_invalidCaptcha_throwsException() {
        var captcha = createMockCaptcha();
        captcha.setText("wrongtext");
        when(captchaService.getCaptchaByByBase64Image(anyString())).thenReturn(captcha);

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(
            "testuser", "captchaImage", "12345", "127.0.0.1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid CAPTCHA");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_nonExistentUser_noEmailSent() {
        var captcha = createMockCaptcha();
        when(captchaService.getCaptchaByByBase64Image(anyString())).thenReturn(captcha);
        when(userService.findUserByUsernameOrEmail("nonexistent")).thenReturn(null);

        passwordResetService.requestPasswordReset("nonexistent", "captchaImage", "12345", "127.0.0.1");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_activeTokenExists_reusesTokenNoNewEmail() {
        User user = createTestUser("testuser", "test@example.com");
        var captcha = createMockCaptcha();
        var existingToken = PasswordResetToken.builder()
            .userId(user.getId())
            .token("existing-token")
            .createdAt(Instant.now().minus(10, ChronoUnit.MINUTES))
            .expiresAt(Instant.now().plus(50, ChronoUnit.MINUTES))
            .build();

        when(captchaService.getCaptchaByByBase64Image(anyString())).thenReturn(captcha);
        when(userService.findUserByUsernameOrEmail("testuser")).thenReturn(user);
        when(passwordResetTokenRepository.findByUserId(user.getId())).thenReturn(Optional.of(existingToken));

        passwordResetService.requestPasswordReset("testuser", "captchaImage", "12345", "127.0.0.1");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).delete(any());
        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void validateResetToken_expiredToken_returnsFalse() {
        var expiredToken = PasswordResetToken.builder()
            .token("test-token")
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        when(passwordResetTokenRepository.findByToken("test-token")).thenReturn(Optional.of(expiredToken));

        boolean valid = passwordResetService.validateResetToken("test-token");

        assertThat(valid).isFalse();
    }

    @Test
    void validateResetToken_validToken_returnsTrue() {
        var validToken = PasswordResetToken.builder()
            .token("test-token")
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .build();

        when(passwordResetTokenRepository.findByToken("test-token")).thenReturn(Optional.of(validToken));

        boolean valid = passwordResetService.validateResetToken("test-token");

        assertThat(valid).isTrue();
    }

    @Test
    void resetPassword_validToken_deletesTokenAndInvalidatesSessions() {
        User user = createTestUser("testuser", "test@example.com");
        var resetToken = PasswordResetToken.builder()
            .token("test-token")
            .userId(user.getId())
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
            .build();

        when(passwordResetTokenRepository.findByToken("test-token")).thenReturn(Optional.of(resetToken));
        when(userService.getLoggedInUserFullNoException()).thenReturn(null);
        when(userService.findUserById(user.getId())).thenReturn(user);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded-password");

        passwordResetService.resetPassword("test-token", "NewPassword123!");

        verify(passwordResetTokenRepository).delete(resetToken);
        verify(userService).invalidateAllUserSessions(user.getId());
        verify(userService).saveUser(argThat(u -> u.getPassword().equals("encoded-password")));
        verify(mailService).sendMail(
            eq("test@example.com"),
            eq("Password Reset Successful - Solar Monitoring"),
            contains("successfully changed")
        );
    }

    @Test
    void resetPassword_weakPassword_throwsException() {
        assertThatThrownBy(() -> passwordResetService.resetPassword("test-token", "weak"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Password must be at least 8 characters");

        verify(passwordResetTokenRepository, never()).delete(any());
        verify(userService, never()).saveUser(any());
    }

    private User createTestUser(String username, String email) {
        User user = new User();
        user.setId("user-id-123");
        user.setName(username);
        user.setMail(email);
        user.setPassword("old-encoded-password");
        return user;
    }

    private Captcha createMockCaptcha() {
        Captcha captcha = new Captcha();
        captcha.setText("12345");
        captcha.setBase64Image("base64image");
        return captcha;
    }
}
