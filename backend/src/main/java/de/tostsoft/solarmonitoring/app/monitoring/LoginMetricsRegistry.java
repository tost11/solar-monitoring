package de.tostsoft.solarmonitoring.app.monitoring;

import de.tostsoft.solarmonitoring.app.service.BruteForceProtectionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LoginMetricsRegistry {

    private final Counter counterLoginAttempts;
    private final Counter counterLoginFailed;
    private final Counter counterLoginSuccessful;
    private final Counter counterAccountLockouts;

    private final BruteForceProtectionService bruteForceProtectionService;

    public LoginMetricsRegistry(MeterRegistry meterRegistry, BruteForceProtectionService bruteForceProtectionService) {
        this.bruteForceProtectionService = bruteForceProtectionService;

        this.counterLoginAttempts = meterRegistry.counter("login.attempts.total");
        this.counterLoginFailed = meterRegistry.counter("login.attempts.failed");
        this.counterLoginSuccessful = meterRegistry.counter("login.attempts.successful");
        this.counterAccountLockouts = meterRegistry.counter("login.lockouts.total");

        Gauge.builder("login.accounts.locked", bruteForceProtectionService,
                BruteForceProtectionService::getCurrentlyLockedAccounts)
            .description("Number of currently locked accounts")
            .register(meterRegistry);
    }

    public void incrementLoginAttempt() {
        this.counterLoginAttempts.increment();
    }

    public void incrementLoginFailed() {
        this.counterLoginFailed.increment();
    }

    public void incrementLoginSuccessful() {
        this.counterLoginSuccessful.increment();
    }

    public void incrementAccountLockout() {
        this.counterAccountLockouts.increment();
    }
}
