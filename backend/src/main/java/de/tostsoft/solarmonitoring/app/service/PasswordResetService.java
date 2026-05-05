package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.PasswordResetToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.PasswordResetTokenRepository;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.passwordReset.tokenValidityMinutes:60}")
    private int tokenValidityMinutes;

    @Value("${fulldomain}")
    private String fulldomain;

    public void requestPasswordReset(String usernameOrEmail, String captchaImage,
                                     String captchaText, String ipAddress) {

        var captcha = captchaService.getCaptchaByByBase64Image(captchaImage);
        if (captcha == null) {
            LOG.warn("Unknown CAPTCHA for password reset request: {}", ipAddress);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CAPTCHA");
        }
        if (!StringUtils.equalsAnyIgnoreCase(captcha.getText(), captchaText)) {
            LOG.warn("Incorrect CAPTCHA for password reset request: {}", ipAddress);
            captchaService.deleteCaptcha(captcha);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CAPTCHA");
        }

        captchaService.deleteCaptcha(captcha);

        try {
            Thread.sleep(50 + (long)(Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String normalized = StringUtils.lowerCase(StringUtils.trim(usernameOrEmail));

        User user = userService.findUserByUsernameOrEmail(normalized);

        if (user != null && StringUtils.isNotBlank(user.getMail())) {
            Instant now = Instant.now();

            Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUserId(user.getId());

            if (existingToken.isPresent() && existingToken.get().getExpiresAt().isAfter(now)) {
                LOG.info("Password reset requested for user: {}, reusing existing active token", user.getName());
            } else {
                String token = UUID.randomUUID().toString();
                Instant expiresAt = now.plus(tokenValidityMinutes, ChronoUnit.MINUTES);

                if (existingToken.isPresent()) {
                    passwordResetTokenRepository.delete(existingToken.get());
                }

                PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .createdAt(now)
                    .expiresAt(expiresAt)
                    .build();

                passwordResetTokenRepository.save(resetToken);

                String resetLink = String.format("%s/reset-password?token=%s", fulldomain, token);

                mailService.sendMail(
                    user.getMail(),
                    "Password Reset Request - Solar Monitoring",
                    String.format(
                        "Hello,\n\n" +
                        "A password reset was requested for your Solar Monitoring account.\n\n" +
                        "To reset your password, click the link below. This link will expire in %d minutes.\n\n" +
                        "%s\n\n" +
                        "If you did not request this reset, please ignore this email. " +
                        "Your password will not be changed.\n\n" +
                        "For security reasons, please do not share this link with anyone.",
                        tokenValidityMinutes,
                        resetLink
                    )
                );

                LOG.info("Password reset requested for user: {}, token sent to email", user.getName());
            }

        } else {
            LOG.info("Password reset requested for non-existent user or user without email: {}", normalized);
        }
    }

    public boolean validateResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
            .map(resetToken -> resetToken.getExpiresAt().isAfter(Instant.now()))
            .orElse(false);
    }

    public void resetPassword(String token, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must be at least 8 characters");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Reset token has expired");
        }

        User user = userService.getLoggedInUserFullNoException();
        if (user == null) {
            user = userService.findUserById(resetToken.getUserId());
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        passwordResetTokenRepository.delete(resetToken);

        userService.invalidateAllUserSessions(user.getId());

        if (StringUtils.isNotBlank(user.getMail())) {
            try {
                mailService.sendMail(
                    user.getMail(),
                    "Password Reset Successful - Solar Monitoring",
                    "Hello,\n\n" +
                    "Your Solar Monitoring account password has been successfully changed.\n\n" +
                    "If you did not make this change, please contact support immediately."
                );
            } catch (Exception e) {
                LOG.error("Failed to send password reset confirmation email: {}", e.getMessage());
            }
        }

        LOG.info("Password reset successful for user: {}", user.getName());
    }
}
