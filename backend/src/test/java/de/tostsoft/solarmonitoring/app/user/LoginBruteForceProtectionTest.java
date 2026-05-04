package de.tostsoft.solarmonitoring.app.user;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.lib.repository.AccountLockoutRepository;
import de.tostsoft.solarmonitoring.lib.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoginBruteForceProtectionTest extends AppBaseTest {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private AccountLockoutRepository accountLockoutRepository;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        loginAttemptRepository.deleteAll();
        accountLockoutRepository.deleteAll();
    }

    @Test
    public void successfulLogin_shouldAllowAccess() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("password");

        var response = doRestRequest("/api/user/login", loginDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jwt");
    }

    @Test
    public void failedLogin_shouldReturnUnauthorized() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        var ex = assertThrows(HttpClientErrorException.class,
            () -> doRestRequest("/api/user/login", loginDTO));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void multipleFailedLogins_shouldRecordAttempts() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < 5; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        Instant windowStart = Instant.now().minus(30, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(
            "testuser", windowStart);

        assertThat(failedCount).isEqualTo(5L);
    }

    @Test
    public void eightFailedLogins_shouldLockAccount() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < 8; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        var lockout = accountLockoutRepository.findByUsername("testuser");
        assertThat(lockout).isPresent();
        assertThat(lockout.get().getFailedAttempts()).isEqualTo(8);
        assertThat(lockout.get().getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    public void lockedAccount_shouldRejectEvenWithCorrectPassword() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < 8; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        loginDTO.setPassword("password");
        var ex = assertThrows(HttpClientErrorException.class,
            () -> doRestRequest("/api/user/login", loginDTO));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void successfulLogin_shouldClearLockout() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < 5; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        assertThat(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(
            "testuser", Instant.now().minus(30, ChronoUnit.MINUTES))).isEqualTo(5L);

        loginDTO.setPassword("password");
        var response = doRestRequest("/api/user/login", loginDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jwt");

        var attempts = loginAttemptRepository.findByUsernameAndTimestampAfter(
            "testuser", Instant.now().minus(1, ChronoUnit.MINUTES));
        long successfulCount = attempts.stream().filter(a -> a.isSuccess()).count();
        assertThat(successfulCount).isGreaterThan(0);
    }

    @Test
    public void nonExistentUser_shouldReturnSameError() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("nonexistent");
        loginDTO.setPassword("anypassword");

        var ex = assertThrows(HttpClientErrorException.class,
            () -> doRestRequest("/api/user/login", loginDTO));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonExistentUser_shouldStillRecordAttempts() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("nonexistent");
        loginDTO.setPassword("anypassword");

        for (int i = 0; i < 5; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        Instant windowStart = Instant.now().minus(30, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(
            "nonexistent", windowStart);

        assertThat(failedCount).isEqualTo(5L);
    }

    @Test
    public void nonExistentUser_canBeLocked() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("nonexistent");
        loginDTO.setPassword("anypassword");

        for (int i = 0; i < 8; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        var lockout = accountLockoutRepository.findByUsername("nonexistent");
        assertThat(lockout).isPresent();
        assertThat(lockout.get().getFailedAttempts()).isEqualTo(8);
    }

    @Test
    public void usernameNormalization_shouldBeCaseInsensitive() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("TestUser");
        loginDTO.setPassword("wrongpassword");

        for (int i = 0; i < 5; i++) {
            assertThrows(HttpClientErrorException.class,
                () -> doRestRequest("/api/user/login", loginDTO));
        }

        Instant windowStart = Instant.now().minus(30, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(
            "testuser", windowStart);

        assertThat(failedCount).isEqualTo(5L);
    }

    @Test
    public void loginAttempts_shouldHaveTimestamp() {
        addUser(false, "testuser");

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setName("testuser");
        loginDTO.setPassword("wrongpassword");

        assertThrows(HttpClientErrorException.class,
            () -> doRestRequest("/api/user/login", loginDTO));

        var attempts = loginAttemptRepository.findByUsernameAndTimestampAfter(
            "testuser", Instant.now().minus(5, ChronoUnit.MINUTES));

        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getTimestamp()).isNotNull();
        assertThat(attempts.get(0).getTimestamp()).isBefore(Instant.now().plus(1, ChronoUnit.SECONDS));
        assertThat(attempts.get(0).isSuccess()).isFalse();
    }
}
