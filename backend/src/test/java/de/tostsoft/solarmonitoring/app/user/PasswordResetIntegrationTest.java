package de.tostsoft.solarmonitoring.app.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.users.PasswordResetConfirmDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.PasswordResetRequestDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.RegisterInfoDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.model.PasswordResetToken;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.testlib.Waiter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class PasswordResetIntegrationTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void passwordResetFlow_completeWorkflow_success() throws Exception {
        User user = addUser(false, "testuser");
        String oldPassword = "password";

        var res = doRestRequest("api/user/register");
        var registerInfo = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
        var captcha = captchaRepository.getCaptchaByBase64Image(registerInfo.getCaptcha());

        PasswordResetRequestDTO resetRequest = new PasswordResetRequestDTO();
        resetRequest.setUsernameOrEmail("testuser");
        resetRequest.setCaptchaImage(captcha.getBase64Image());
        resetRequest.setCaptchaText(captcha.getText());

        var resetResponse = doRestRequest("/api/user/password-reset/request", resetRequest, HttpMethod.POST);
        assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getSize() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, 10 * 1000);

        var mailHogResponse = mailhogTestService.fetchMails();
        assertThat(mailHogResponse.getSize()).isEqualTo(1);

        var mail = mailHogResponse.getMailList().get(0);
        assertThat(mail.getSubject()).isEqualTo("Password Reset Request - Solar Monitoring");
        assertThat(mail.getTo()).contains("testuser@local.host");
        assertThat(mail.getContent()).contains("/reset-password?token=");
        assertThat(mail.getContent()).contains("expire in 60 minutes");

        Pattern pattern = Pattern.compile("/reset-password\\?token=([a-f0-9\\-]+)");
        Matcher matcher = pattern.matcher(mail.getContent());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        var validateResponse = doRestRequest("/api/user/password-reset/validate/" + token);
        assertThat(validateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var validationResult = objectMapper.readValue(validateResponse.getBody(), Map.class);
        assertThat(validationResult.get("valid")).isEqualTo(true);

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken(token);
        confirmDTO.setNewPassword("NewPassword123!");

        var confirmResponse = doRestRequest("/api/user/password-reset/confirm", confirmDTO, HttpMethod.POST);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getSize() > 1;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, 10 * 1000);
        mailHogResponse = mailhogTestService.fetchMails();
        assertThat(mailHogResponse.getSize()).isGreaterThanOrEqualTo(2);

        var confirmMail = mailHogResponse.getMailList().stream()
            .filter(m -> m.getSubject().equals("Password Reset Successful - Solar Monitoring"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Confirmation email not found"));
        assertThat(confirmMail.getSubject()).isEqualTo("Password Reset Successful - Solar Monitoring");

        UserLoginDTO loginOldPassword = UserLoginDTO.builder()
            .name("testuser")
            .password(oldPassword)
            .build();

        try {
            doRestRequest("/api/user/login", loginOldPassword, HttpMethod.POST);
            throw new AssertionError("Expected old password to fail");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("401");
        }

        UserLoginDTO loginNewPassword = UserLoginDTO.builder()
            .name("testuser")
            .password("NewPassword123!")
            .build();

        var newLoginResponse = doRestRequest("/api/user/login", loginNewPassword, HttpMethod.POST);
        assertThat(newLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(passwordResetTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    public void passwordResetRequest_activeTokenExists_noNewEmailSent() throws Exception {
        User user = addUser(false, "testuser");

        var captcha1 = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest1 = new PasswordResetRequestDTO();
        resetRequest1.setUsernameOrEmail("testuser");
        resetRequest1.setCaptchaImage(captcha1.getBase64Image());
        resetRequest1.setCaptchaText(captcha1.getText());

        doRestRequest("/api/user/password-reset/request", resetRequest1, HttpMethod.POST);

        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getSize() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, 10 * 1000);
        assertThat(mailhogTestService.fetchMails().getSize()).isEqualTo(1);

        var captcha2 = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest2 = new PasswordResetRequestDTO();
        resetRequest2.setUsernameOrEmail("testuser");
        resetRequest2.setCaptchaImage(captcha2.getBase64Image());
        resetRequest2.setCaptchaText(captcha2.getText());

        doRestRequest("/api/user/password-reset/request", resetRequest2, HttpMethod.POST);

        Thread.sleep(2000);
        assertThat(mailhogTestService.fetchMails().getSize()).isEqualTo(1);

        assertThat(passwordResetTokenRepository.findByUserId(user.getId())).isPresent();
        var tokens = passwordResetTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
    }

    @Test
    public void passwordResetConfirm_expiredToken_rejected() throws Exception {
        User user = addUser(false, "testuser");

        PasswordResetToken expiredToken = PasswordResetToken.builder()
            .userId(user.getId())
            .token(UUID.randomUUID().toString())
            .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        passwordResetTokenRepository.save(expiredToken);

        var validateResponse = doRestRequest("/api/user/password-reset/validate/" + expiredToken.getToken());
        assertThat(validateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var validationResult = objectMapper.readValue(validateResponse.getBody(), Map.class);
        assertThat(validationResult.get("valid")).isEqualTo(false);

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken(expiredToken.getToken());
        confirmDTO.setNewPassword("NewPassword123!");

        try {
            doRestRequest("/api/user/password-reset/confirm", confirmDTO, HttpMethod.POST);
            throw new AssertionError("Expected BadRequest exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("400");
            assertThat(e.getMessage()).contains("expired");
        }
    }

    @Test
    public void passwordResetConfirm_invalidToken_rejected() throws Exception {
        var validateResponse = doRestRequest("/api/user/password-reset/validate/invalid-token-12345");
        assertThat(validateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var validationResult = objectMapper.readValue(validateResponse.getBody(), Map.class);
        assertThat(validationResult.get("valid")).isEqualTo(false);

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken("invalid-token-12345");
        confirmDTO.setNewPassword("NewPassword123!");

        try {
            doRestRequest("/api/user/password-reset/confirm", confirmDTO, HttpMethod.POST);
            throw new AssertionError("Expected BadRequest exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("400");
            assertThat(e.getMessage()).contains("Invalid or expired");
        }
    }

    @Test
    public void passwordResetRequest_nonExistentUser_sameResponseAsValidUser() throws Exception {
        var captcha1 = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest1 = new PasswordResetRequestDTO();
        resetRequest1.setUsernameOrEmail("nonexistent");
        resetRequest1.setCaptchaImage(captcha1.getBase64Image());
        resetRequest1.setCaptchaText(captcha1.getText());

        long start1 = System.currentTimeMillis();
        var response1 = doRestRequest("/api/user/password-reset/request", resetRequest1, HttpMethod.POST);
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body1 = objectMapper.readValue(response1.getBody(), Map.class);
        assertThat(body1.get("message")).isEqualTo("If an account exists with that username or email, a password reset link has been sent.");

        addUser(false, "testuser");
        var captcha2 = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest2 = new PasswordResetRequestDTO();
        resetRequest2.setUsernameOrEmail("testuser");
        resetRequest2.setCaptchaImage(captcha2.getBase64Image());
        resetRequest2.setCaptchaText(captcha2.getText());

        long start2 = System.currentTimeMillis();
        var response2 = doRestRequest("/api/user/password-reset/request", resetRequest2, HttpMethod.POST);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body2 = objectMapper.readValue(response2.getBody(), Map.class);
        assertThat(body2.get("message")).isEqualTo(body1.get("message"));

        assertThat(Math.abs(duration1 - duration2)).isLessThan(300);

        Thread.sleep(2000);
        var mailHogResponse = mailhogTestService.fetchMails();
        assertThat(mailHogResponse.getSize()).isEqualTo(1);
    }

    @Test
    public void passwordReset_invalidatesAllActiveSessions() throws Exception {
        User user = addUser(false, "testuser");
        String jwtToken = signIn("testuser", "password");

        var headers = new HashMap<String, String>();
        headers.put("Cookie", "jwt=" + jwtToken);
        var userResponse = doRequest("/api/user", HttpMethod.GET, headers);
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var captcha = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest = new PasswordResetRequestDTO();
        resetRequest.setUsernameOrEmail("testuser");
        resetRequest.setCaptchaImage(captcha.getBase64Image());
        resetRequest.setCaptchaText(captcha.getText());

        doRestRequest("/api/user/password-reset/request", resetRequest, HttpMethod.POST);

        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getSize() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, 10 * 1000);
        var mail = mailhogTestService.fetchMails().getMailList().get(0);

        Pattern pattern = Pattern.compile("/reset-password\\?token=([a-f0-9\\-]+)");
        Matcher matcher = pattern.matcher(mail.getContent());
        matcher.find();
        String token = matcher.group(1);

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken(token);
        confirmDTO.setNewPassword("NewPassword123!");

        doRestRequest("/api/user/password-reset/confirm", confirmDTO, HttpMethod.POST);

        try {
            doRequest("/api/user", HttpMethod.GET, headers);
            throw new AssertionError("Expected old JWT session to be invalid");
        } catch (Exception e) {
            assertThat(e.getMessage()).containsAnyOf("401", "403");
        }

        String newJwtToken = signIn("testuser", "NewPassword123!");
        assertThat(newJwtToken).isNotNull();

        headers.put("Cookie", "jwt=" + newJwtToken);
        var newSessionResponse = doRequest("/api/user", HttpMethod.GET, headers);
        assertThat(newSessionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void passwordResetRequest_usingEmail_success() throws Exception {
        User user = addUser(false, "testuser");
        assertThat(user.getMail()).isEqualTo("testuser@local.host");

        var captcha = getCaptchaForTest();
        PasswordResetRequestDTO resetRequest = new PasswordResetRequestDTO();
        resetRequest.setUsernameOrEmail("testuser@local.host");
        resetRequest.setCaptchaImage(captcha.getBase64Image());
        resetRequest.setCaptchaText(captcha.getText());

        var response = doRestRequest("/api/user/password-reset/request", resetRequest, HttpMethod.POST);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Waiter.waitToHappen(() -> {
            try {
                return mailhogTestService.fetchMails().getSize() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, 10 * 1000);
        var mailHogResponse = mailhogTestService.fetchMails();
        assertThat(mailHogResponse.getSize()).isEqualTo(1);

        var mail = mailHogResponse.getMailList().get(0);
        assertThat(mail.getTo()).contains("testuser@local.host");
    }

    private Captcha getCaptchaForTest() {
        try {
            var res = doRestRequest("api/user/register");
            var registerInfo = objectMapper.readValue(res.getBody(), RegisterInfoDTO.class);
            return captchaRepository.getCaptchaByBase64Image(registerInfo.getCaptcha());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
