package de.tostsoft.solarmonitoring.updater.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatisticsMetricsRegistry {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> failureCounters;

    public StatisticsMetricsRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.failureCounters = new ConcurrentHashMap<>();
    }

    public void incrementStatisticsFailure(String systemId) {
        failureCounters.computeIfAbsent(systemId, id ->
            Counter.builder("statistics.systemupdates.failures")
                .tag("system_id", id)
                .description("Number of failed statistics update attempts per system")
                .register(meterRegistry)
        ).increment();
    }
}
