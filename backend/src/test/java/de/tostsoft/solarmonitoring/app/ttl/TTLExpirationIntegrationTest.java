package de.tostsoft.solarmonitoring.app.ttl;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.repository.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TTLExpirationIntegrationTest extends AppBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(TTLExpirationIntegrationTest.class);

    @Autowired
    private CaptchaRepository captchaRepository;

    @Autowired
    private RegisterUserRepository registerUserRepository;

    @Autowired
    private JWTSessionTokenRepository jwtSessionTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private AccountLockoutRepository accountLockoutRepository;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testAllTTLIndexes_expiredEntities_automaticallyDeleted() throws InterruptedException {

        // MongoDB TTL calculation: expiresAt = field_value + expireAfterSeconds
        // To expire immediately, we need: field_value = now - expireAfterSeconds - buffer

        Instant now = Instant.now();

        // 1. Create expired Captcha (TTL: 3600 seconds = 1 hour)
        Instant captchaExpiredTime = now.minus(3600 + 10, ChronoUnit.SECONDS);
        Captcha captcha = Captcha.builder()
                .base64Image("test-captcha-" + UUID.randomUUID())
                .text("12345")
                .createdAt(captchaExpiredTime)
                .build();
        captcha = captchaRepository.save(captcha);
        String captchaId = captcha.getId();

        // 2. Create expired RegisterUser (TTL: 86400 seconds = 24 hours)
        Instant registerUserExpiredTime = now.minus(86400 + 10, ChronoUnit.SECONDS);
        RegisterUser registerUser = RegisterUser.builder()
                .name("testuser-" + UUID.randomUUID().toString().replace("-", ""))
                .viewName("Test User")
                .mail("test-" + UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .createdAt(registerUserExpiredTime)
                .build();
        registerUser = registerUserRepository.save(registerUser);
        String registerUserId = registerUser.getId();

        // 3. Create expired JWTSessionToken (TTL: 0 seconds = expire at exact timestamp)
        Instant jwtExpiredTime = now.minus(10, ChronoUnit.SECONDS);
        User user = addUser(true);
        JWTSessionToken sessionToken = JWTSessionToken.builder()
                .validUntil(jwtExpiredTime)
                .ownedBy(user)
                .build();
        sessionToken = jwtSessionTokenRepository.save(sessionToken);
        String sessionTokenId = sessionToken.getId();

        // 4. Create expired PasswordResetToken (TTL: 3600 seconds = 1 hour)
        Instant resetTokenExpiredTime = now.minus(3600 + 10, ChronoUnit.SECONDS);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(new ObjectId().toString())
                .token(UUID.randomUUID().toString())
                .createdAt(resetTokenExpiredTime)
                .expiresAt(resetTokenExpiredTime)
                .build();
        resetToken = passwordResetTokenRepository.save(resetToken);
        String resetTokenId = resetToken.getId();

        // 5. Create expired LoginAttempt (TTL: 86400 seconds = 1 day)
        Instant loginAttemptExpiredTime = now.minus(86400 + 10, ChronoUnit.SECONDS);
        LoginAttempt loginAttempt = LoginAttempt.builder()
                .username("testuser")
                .ipAddress("192.168.1.1")
                .timestamp(loginAttemptExpiredTime)
                .success(false)
                .build();
        loginAttempt = loginAttemptRepository.save(loginAttempt);
        String loginAttemptId = loginAttempt.getId();

        // 6. Create expired AccountLockout (TTL: 604800 seconds = 7 days)
        Instant accountLockoutExpiredTime = now.minus(604800 + 10, ChronoUnit.SECONDS);
        AccountLockout accountLockout = AccountLockout.builder()
                .username("testuser-lockout-" + UUID.randomUUID())
                .lockedAt(accountLockoutExpiredTime)
                .lockedUntil(accountLockoutExpiredTime)
                .expiresAt(accountLockoutExpiredTime)
                .failedAttempts(5)
                .lastAttemptIp("192.168.1.1")
                .build();
        accountLockout = accountLockoutRepository.save(accountLockout);
        String accountLockoutId = accountLockout.getId();

        // Verify all entities exist before TTL cleanup
        assertThat(captchaRepository.findById(captchaId)).isPresent();
        assertThat(registerUserRepository.findById(registerUserId)).isPresent();
        assertThat(jwtSessionTokenRepository.findById(sessionTokenId)).isPresent();
        assertThat(passwordResetTokenRepository.findById(resetTokenId)).isPresent();
        assertThat(loginAttemptRepository.findById(loginAttemptId)).isPresent();
        assertThat(accountLockoutRepository.findById(accountLockoutId)).isPresent();

        LOG.info("All 6 entities created with expired timestamps. Waiting 130 seconds for MongoDB TTL cleanup...");

        // Wait for MongoDB TTL background task to run (runs every 60 seconds)
        Thread.sleep(130_000);

        LOG.info("Wait complete. Verifying entities were deleted by MongoDB TTL indexes...");

        // Verify all expired entities were deleted by MongoDB TTL
        assertThat(captchaRepository.findById(captchaId)).isEmpty();
        assertThat(registerUserRepository.findById(registerUserId)).isEmpty();
        assertThat(jwtSessionTokenRepository.findById(sessionTokenId)).isEmpty();
        assertThat(passwordResetTokenRepository.findById(resetTokenId)).isEmpty();
        assertThat(loginAttemptRepository.findById(loginAttemptId)).isEmpty();
        assertThat(accountLockoutRepository.findById(accountLockoutId)).isEmpty();

        LOG.info("SUCCESS: All 6 expired entities were automatically deleted by MongoDB TTL indexes");
    }

    @Test
    public void testAllTTLIndexes_nearExpirationEntities_notDeleted() throws InterruptedException {

        // Create entities that are close to expiring but still valid
        // MongoDB TTL calculation: expiresAt = field_value + expireAfterSeconds
        // To be close but not expired: field_value = now - expireAfterSeconds + small_buffer

        Instant now = Instant.now();
        int bufferSeconds = 300; // 5 minutes before expiration

        // 1. Create near-expiration Captcha (TTL: 3600 seconds = 1 hour)
        // Will expire in ~5 minutes
        Instant captchaNearExpireTime = now.minus(3600 - bufferSeconds, ChronoUnit.SECONDS);
        Captcha captcha = Captcha.builder()
                .base64Image("test-captcha-near-" + UUID.randomUUID())
                .text("12345")
                .createdAt(captchaNearExpireTime)
                .build();
        captcha = captchaRepository.save(captcha);
        String captchaId = captcha.getId();

        // 2. Create near-expiration RegisterUser (TTL: 86400 seconds = 24 hours)
        // Will expire in ~5 minutes
        Instant registerUserNearExpireTime = now.minus(86400 - bufferSeconds, ChronoUnit.SECONDS);
        RegisterUser registerUser = RegisterUser.builder()
                .name("testuser-near-" + UUID.randomUUID().toString().replace("-", ""))
                .viewName("Test User Near")
                .mail("test-near-" + UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .createdAt(registerUserNearExpireTime)
                .build();
        registerUser = registerUserRepository.save(registerUser);
        String registerUserId = registerUser.getId();

        // 3. Create near-expiration JWTSessionToken (TTL: 0 seconds = expire at exact timestamp)
        // Will expire in ~5 minutes
        Instant jwtNearExpireTime = now.plus(bufferSeconds, ChronoUnit.SECONDS);
        User user = addUser(true);
        JWTSessionToken sessionToken = JWTSessionToken.builder()
                .validUntil(jwtNearExpireTime)
                .ownedBy(user)
                .build();
        sessionToken = jwtSessionTokenRepository.save(sessionToken);
        String sessionTokenId = sessionToken.getId();

        // 4. Create near-expiration PasswordResetToken (TTL: 3600 seconds = 1 hour)
        // Will expire in ~5 minutes
        Instant resetTokenNearExpireTime = now.minus(3600 - bufferSeconds, ChronoUnit.SECONDS);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(new ObjectId().toString())
                .token(UUID.randomUUID().toString())
                .createdAt(resetTokenNearExpireTime)
                .expiresAt(resetTokenNearExpireTime)
                .build();
        resetToken = passwordResetTokenRepository.save(resetToken);
        String resetTokenId = resetToken.getId();

        // 5. Create near-expiration LoginAttempt (TTL: 86400 seconds = 1 day)
        // Will expire in ~5 minutes
        Instant loginAttemptNearExpireTime = now.minus(86400 - bufferSeconds, ChronoUnit.SECONDS);
        LoginAttempt loginAttempt = LoginAttempt.builder()
                .username("testuser-near")
                .ipAddress("192.168.1.2")
                .timestamp(loginAttemptNearExpireTime)
                .success(false)
                .build();
        loginAttempt = loginAttemptRepository.save(loginAttempt);
        String loginAttemptId = loginAttempt.getId();

        // 6. Create near-expiration AccountLockout (TTL: 604800 seconds = 7 days)
        // Will expire in ~5 minutes
        Instant accountLockoutNearExpireTime = now.minus(604800 - bufferSeconds, ChronoUnit.SECONDS);
        AccountLockout accountLockout = AccountLockout.builder()
                .username("testuser-lockout-near-" + UUID.randomUUID())
                .lockedAt(accountLockoutNearExpireTime)
                .lockedUntil(accountLockoutNearExpireTime)
                .expiresAt(accountLockoutNearExpireTime)
                .failedAttempts(5)
                .lastAttemptIp("192.168.1.2")
                .build();
        accountLockout = accountLockoutRepository.save(accountLockout);
        String accountLockoutId = accountLockout.getId();

        // Verify all entities exist before waiting
        assertThat(captchaRepository.findById(captchaId)).isPresent();
        assertThat(registerUserRepository.findById(registerUserId)).isPresent();
        assertThat(jwtSessionTokenRepository.findById(sessionTokenId)).isPresent();
        assertThat(passwordResetTokenRepository.findById(resetTokenId)).isPresent();
        assertThat(loginAttemptRepository.findById(loginAttemptId)).isPresent();
        assertThat(accountLockoutRepository.findById(accountLockoutId)).isPresent();

        LOG.info("All 6 entities created with near-expiration timestamps (expire in ~5 minutes). Waiting 130 seconds for MongoDB TTL cleanup...");

        // Wait for MongoDB TTL background task to run (runs every 60 seconds)
        Thread.sleep(130_000);

        LOG.info("Wait complete. Verifying entities still exist (were NOT deleted by TTL)...");

        // Verify all near-expiration entities still exist (were NOT deleted)
        assertThat(captchaRepository.findById(captchaId)).isPresent();
        assertThat(registerUserRepository.findById(registerUserId)).isPresent();
        assertThat(jwtSessionTokenRepository.findById(sessionTokenId)).isPresent();
        assertThat(passwordResetTokenRepository.findById(resetTokenId)).isPresent();
        assertThat(loginAttemptRepository.findById(loginAttemptId)).isPresent();
        assertThat(accountLockoutRepository.findById(accountLockoutId)).isPresent();

        LOG.info("SUCCESS: All 6 near-expiration entities still exist - TTL correctly preserves non-expired documents");
    }
}
