package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.AccountLockout;
import de.tostsoft.solarmonitoring.lib.model.LoginAttempt;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.AccountLockoutRepository;
import de.tostsoft.solarmonitoring.lib.repository.LoginAttemptRepository;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
class BruteForceProtectionServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private AccountLockoutRepository accountLockoutRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private BruteForceProtectionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAccountAttempts", 8);
        ReflectionTestUtils.setField(service, "accountWindowMinutes", 30);
        ReflectionTestUtils.setField(service, "accountLockoutMinutes", 15);
        ReflectionTestUtils.setField(service, "ipBlockingEnabled", true);
        ReflectionTestUtils.setField(service, "maxIpAttempts", 30);
        ReflectionTestUtils.setField(service, "ipWindowMinutes", 30);
    }

    @Test
    void checkLoginAttempt_shouldAllowWhenNoLockout() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", "192.168.1.1");

        verify(accountLockoutRepository).findByUsername("testuser");
        verify(loginAttemptRepository).countByIpAddressAndTimestampAfterAndSuccessFalse(eq("192.168.1.1"), any(Instant.class));
    }

    @Test
    void checkLoginAttempt_shouldBlockWhenAccountLocked() {
        AccountLockout lockout = AccountLockout.builder()
            .username("testuser")
            .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.of(lockout));

        assertThatThrownBy(() -> service.checkLoginAttempt("testuser", "192.168.1.1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid username or password")
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void checkLoginAttempt_shouldAllowWhenLockoutExpired() {
        AccountLockout lockout = AccountLockout.builder()
            .username("testuser")
            .lockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES))
            .build();

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.of(lockout));
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", "192.168.1.1");

        verify(accountLockoutRepository).findByUsername("testuser");
    }

    @Test
    void checkLoginAttempt_shouldBlockWhenIpExceedsLimit() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(30L);

        assertThatThrownBy(() -> service.checkLoginAttempt("testuser", "192.168.1.1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid username or password")
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void recordFailedLogin_shouldSaveAttempt() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(3L);

        service.recordFailedLogin("testuser", "192.168.1.1", "Mozilla/5.0", null);

        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());

        LoginAttempt saved = attemptCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void recordFailedLogin_shouldLockAccountAfterMaxAttempts() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(8L);
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        User existingUser = new User();
        existingUser.setMail("test@example.com");

        service.recordFailedLogin("testuser", "192.168.1.1", "Mozilla/5.0", existingUser);

        ArgumentCaptor<AccountLockout> lockoutCaptor = ArgumentCaptor.forClass(AccountLockout.class);
        verify(accountLockoutRepository).save(lockoutCaptor.capture());

        AccountLockout saved = lockoutCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getFailedAttempts()).isEqualTo(8);
        assertThat(saved.getLastAttemptIp()).isEqualTo("192.168.1.1");
        assertThat(saved.getLockedUntil()).isAfter(Instant.now());
        assertThat(saved.getLockedUntil()).isBefore(Instant.now().plus(16, ChronoUnit.MINUTES));
    }

    @Test
    void recordFailedLogin_shouldSendEmailWhenUserExists() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(8L);
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        User existingUser = new User();
        existingUser.setMail("test@example.com");

        service.recordFailedLogin("testuser", "192.168.1.1", "Mozilla/5.0", existingUser);

        verify(mailService).sendMail(
            eq("test@example.com"),
            eq("Security Alert: Account Temporarily Locked"),
            contains("temporarily locked")
        );
    }

    @Test
    void recordFailedLogin_shouldNotSendEmailWhenUserDoesNotExist() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(8L);
        when(accountLockoutRepository.findByUsername("fakeuser")).thenReturn(Optional.empty());

        service.recordFailedLogin("fakeuser", "192.168.1.1", "Mozilla/5.0", null);

        verify(accountLockoutRepository).save(any(AccountLockout.class));
        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void recordFailedLogin_shouldUpdateExistingLockout() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(10L);

        AccountLockout existingLockout = AccountLockout.builder()
            .id("lockout123")
            .username("testuser")
            .lockedUntil(Instant.now().plus(5, ChronoUnit.MINUTES))
            .failedAttempts(8)
            .build();

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.of(existingLockout));

        service.recordFailedLogin("testuser", "192.168.1.2", "Mozilla/5.0", null);

        ArgumentCaptor<AccountLockout> lockoutCaptor = ArgumentCaptor.forClass(AccountLockout.class);
        verify(accountLockoutRepository).save(lockoutCaptor.capture());

        AccountLockout updated = lockoutCaptor.getValue();
        assertThat(updated.getId()).isEqualTo("lockout123");
        assertThat(updated.getFailedAttempts()).isEqualTo(10);
        assertThat(updated.getLastAttemptIp()).isEqualTo("192.168.1.2");
    }

    @Test
    void recordSuccessfulLogin_shouldSaveSuccessfulAttempt() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        service.recordSuccessfulLogin("testuser", "192.168.1.1", "Mozilla/5.0");

        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());

        LoginAttempt saved = attemptCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void recordSuccessfulLogin_shouldClearExistingLockout() {
        AccountLockout existingLockout = AccountLockout.builder()
            .username("testuser")
            .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.of(existingLockout));

        service.recordSuccessfulLogin("testuser", "192.168.1.1", "Mozilla/5.0");

        verify(accountLockoutRepository).delete(existingLockout);
    }

    @Test
    void normalizeUsername_shouldLowercaseAndTrim() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("  TestUser  ", "192.168.1.1");

        verify(accountLockoutRepository).findByUsername("testuser");
    }

    @Test
    void ipv4Address_shouldReturnAsIs() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(eq("192.168.1.100"), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", "192.168.1.100");

        verify(loginAttemptRepository).countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("192.168.1.100"),
            any(Instant.class)
        );
    }

    @Test
    void ipv6Address_shouldExtractPrefix64() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("2001:db8:85a3:0::/64"), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", "2001:db8:85a3:0:0:8a2e:370:7334");

        verify(loginAttemptRepository).countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("2001:db8:85a3:0::/64"),
            any(Instant.class)
        );
    }

    @Test
    void recordFailedLogin_shouldNormalizeIpv6ToPrefix() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(3L);

        service.recordFailedLogin("testuser", "2001:db8:85a3:0:0:8a2e:370:7334", "Mozilla/5.0", null);

        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());

        LoginAttempt saved = attemptCaptor.getValue();
        assertThat(saved.getIpAddress()).isEqualTo("2001:db8:85a3:0::/64");
    }

    @Test
    void getCurrentlyLockedAccounts_shouldReturnCount() {
        when(accountLockoutRepository.countByLockedUntilAfter(any(Instant.class))).thenReturn(5L);

        long locked = service.getCurrentlyLockedAccounts();

        assertThat(locked).isEqualTo(5L);
        verify(accountLockoutRepository).countByLockedUntilAfter(any(Instant.class));
    }

    @Test
    void recordFailedLogin_shouldHandleMailServiceException() {
        when(loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(anyString(), any(Instant.class)))
            .thenReturn(8L);
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        User existingUser = new User();
        existingUser.setMail("test@example.com");

        doThrow(new RuntimeException("Mail server down")).when(mailService)
            .sendMail(anyString(), anyString(), anyString());

        service.recordFailedLogin("testuser", "192.168.1.1", "Mozilla/5.0", existingUser);

        verify(accountLockoutRepository).save(any(AccountLockout.class));
    }

    @Test
    void nullIpAddress_shouldHandleGracefully() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("unknown"), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", null);

        verify(loginAttemptRepository).countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("unknown"),
            any(Instant.class)
        );
    }

    @Test
    void malformedIpv6_shouldReturnAsIs() {
        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("::1"), any(Instant.class)))
            .thenReturn(5L);

        service.checkLoginAttempt("testuser", "::1");

        verify(loginAttemptRepository).countByIpAddressAndTimestampAfterAndSuccessFalse(
            eq("::1"),
            any(Instant.class)
        );
    }

    @Test
    void checkLoginAttempt_whenIpBlockingDisabled_shouldNotCheckIp() {
        ReflectionTestUtils.setField(service, "ipBlockingEnabled", false);

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        service.checkLoginAttempt("testuser", "192.168.1.1");

        verify(accountLockoutRepository).findByUsername("testuser");
        verify(loginAttemptRepository, never()).countByIpAddressAndTimestampAfterAndSuccessFalse(
            anyString(), any(Instant.class));
    }

    @Test
    void checkLoginAttempt_whenIpBlockingDisabled_shouldAllowHighIpAttempts() {
        ReflectionTestUtils.setField(service, "ipBlockingEnabled", false);

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        lenient().when(loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(
            anyString(), any(Instant.class)))
            .thenReturn(100L);

        service.checkLoginAttempt("testuser", "192.168.1.1");

        verify(accountLockoutRepository).findByUsername("testuser");
    }

    @Test
    void checkLoginAttempt_whenIpBlockingDisabled_butAccountLocked_shouldStillBlock() {
        ReflectionTestUtils.setField(service, "ipBlockingEnabled", false);

        AccountLockout lockout = AccountLockout.builder()
            .username("testuser")
            .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

        when(accountLockoutRepository.findByUsername("testuser")).thenReturn(Optional.of(lockout));

        assertThatThrownBy(() -> service.checkLoginAttempt("testuser", "192.168.1.1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid username or password");
    }
}
