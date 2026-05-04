package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.monitoring.LoginMetricsRegistry;
import de.tostsoft.solarmonitoring.lib.model.AccountLockout;
import de.tostsoft.solarmonitoring.lib.model.LoginAttempt;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.AccountLockoutRepository;
import de.tostsoft.solarmonitoring.lib.repository.LoginAttemptRepository;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class BruteForceProtectionService {

    private static final Logger LOG = LoggerFactory.getLogger(BruteForceProtectionService.class);

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private AccountLockoutRepository accountLockoutRepository;

    @Autowired
    private MailService mailService;


    @Value("${security.bruteforce.account.maxAttempts:8}")
    private int maxAccountAttempts;

    @Value("${security.bruteforce.account.windowMinutes:30}")
    private int accountWindowMinutes;

    @Value("${security.bruteforce.account.lockoutMinutes:15}")
    private int accountLockoutMinutes;

    @Value("${security.bruteforce.ip.enabled:true}")
    private boolean ipBlockingEnabled;

    @Value("${security.bruteforce.ip.maxAttempts:30}")
    private int maxIpAttempts;

    @Value("${security.bruteforce.ip.windowMinutes:30}")
    private int ipWindowMinutes;

    public void checkLoginAttempt(String username, String ipAddress) {
        String normalizedUsername = StringUtils.lowerCase(StringUtils.trim(username));
        String normalizedIp = normalizeIpAddress(ipAddress);

        if (isAccountLocked(normalizedUsername)) {
            LOG.warn("Login attempt for locked account: {}", normalizedUsername);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (ipBlockingEnabled && isIpBlocked(normalizedIp)) {
            LOG.warn("Login attempt from blocked IP: {}", normalizedIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public void recordFailedLogin(String username, String ipAddress, String userAgent, User existingUser) {
        String normalizedUsername = StringUtils.lowerCase(StringUtils.trim(username));
        String normalizedIp = normalizeIpAddress(ipAddress);

        LoginAttempt attempt = LoginAttempt.builder()
            .username(normalizedUsername)
            .ipAddress(normalizedIp)
            .success(false)
            .timestamp(Instant.now())
            .userAgent(userAgent)
            .build();
        loginAttemptRepository.save(attempt);

        Instant windowStart = Instant.now().minus(accountWindowMinutes, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countByUsernameAndTimestampAfterAndSuccessFalse(
            normalizedUsername, windowStart);

        if (failedCount >= maxAccountAttempts) {
            lockAccount(normalizedUsername, normalizedIp, (int) failedCount, existingUser);
        }

        LOG.info("Failed login attempt: username={}, ip={}, failedCount={}",
            normalizedUsername, normalizedIp, failedCount);
    }

    public void recordSuccessfulLogin(String username, String ipAddress, String userAgent) {
        String normalizedUsername = StringUtils.lowerCase(StringUtils.trim(username));
        String normalizedIp = normalizeIpAddress(ipAddress);

        LoginAttempt attempt = LoginAttempt.builder()
            .username(normalizedUsername)
            .ipAddress(normalizedIp)
            .success(true)
            .timestamp(Instant.now())
            .userAgent(userAgent)
            .build();
        loginAttemptRepository.save(attempt);

        accountLockoutRepository.findByUsername(normalizedUsername)
            .ifPresent(lockout -> {
                accountLockoutRepository.delete(lockout);
                LOG.info("Cleared lockout for account: {}", normalizedUsername);
            });

        LOG.info("Successful login: username={}, ip={}", normalizedUsername, normalizedIp);
    }

    private boolean isAccountLocked(String normalizedUsername) {
        return accountLockoutRepository.findByUsername(normalizedUsername)
            .map(lockout -> lockout.getLockedUntil().isAfter(Instant.now()))
            .orElse(false);
    }

    private boolean isIpBlocked(String normalizedIp) {
        Instant windowStart = Instant.now().minus(ipWindowMinutes, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countByIpAddressAndTimestampAfterAndSuccessFalse(
            normalizedIp, windowStart);
        return failedCount >= maxIpAttempts;
    }

    private void lockAccount(String normalizedUsername, String ipAddress, int failedAttempts, User existingUser) {
        Instant lockedUntil = Instant.now().plus(accountLockoutMinutes, ChronoUnit.MINUTES);
        Instant now = Instant.now();

        AccountLockout lockout = accountLockoutRepository.findByUsername(normalizedUsername)
            .map(existing -> {
                existing.setLockedUntil(lockedUntil);
                existing.setLockedAt(now);
                existing.setFailedAttempts(failedAttempts);
                existing.setLastAttemptIp(ipAddress);
                existing.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
                return existing;
            })
            .orElse(AccountLockout.builder()
                .username(normalizedUsername)
                .lockedUntil(lockedUntil)
                .lockedAt(now)
                .failedAttempts(failedAttempts)
                .lastAttemptIp(ipAddress)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .build());

        accountLockoutRepository.save(lockout);

        LOG.warn("Account locked: username={}, ip={}, attempts={}, lockedUntil={}",
            normalizedUsername, ipAddress, failedAttempts, lockedUntil);

        if (existingUser != null && StringUtils.isNotBlank(existingUser.getMail())) {
            try {
                mailService.sendMail(
                    existingUser.getMail(),
                    "Security Alert: Account Temporarily Locked",
                    String.format(
                        "Your account has been temporarily locked due to %d failed login attempts.\n\n" +
                        "The account will be automatically unlocked at %s (in %d minutes).\n\n" +
                        "If this wasn't you, please change your password immediately after unlocking.\n\n" +
                        "Last attempt from IP: %s",
                        failedAttempts,
                        lockedUntil.toString(),
                        accountLockoutMinutes,
                        ipAddress
                    )
                );
            } catch (Exception e) {
                LOG.error("Failed to send lockout notification email to {}: {}",
                    existingUser.getMail(), e.getMessage());
            }
        }
    }

    private String normalizeIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }

        if (ipAddress.contains(":")) {
            return extractIPv6Prefix(ipAddress, 64);
        }

        return ipAddress;
    }

    private String extractIPv6Prefix(String ipv6, int prefixLength) {
        try {
            String[] segments = ipv6.split(":");
            if (segments.length < 4) {
                return ipv6;
            }
            return String.format("%s:%s:%s:%s::/64",
                segments[0], segments[1], segments[2], segments[3]);
        } catch (Exception e) {
            LOG.warn("Failed to parse IPv6 address: {}", ipv6);
            return ipv6;
        }
    }

    public long getCurrentlyLockedAccounts() {
        return accountLockoutRepository.countByLockedUntilAfter(Instant.now());
    }
}
