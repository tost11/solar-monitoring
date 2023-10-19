package de.tostsoft.solarmonitoring.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ApiMeterRegistry {

    private final Counter counterApiEndpiontCallData;
    private final Counter counterApiEndpiontCallDataSuccessFul;
    private final Counter counterSolarSamples;

    public ApiMeterRegistry(MeterRegistry registry) {
        this.counterApiEndpiontCallData = registry.counter("api.solar.calls");
        this.counterApiEndpiontCallDataSuccessFul = registry.counter("api.solar.calls.successful");
        this.counterSolarSamples = registry.counter("api.soar.samples");
    }

    public void incrementApiEndpointCallData() {
        this.counterApiEndpiontCallData.increment();
    }

    public void incrementApiEndpointCallDataSuccessful() {
        this.counterApiEndpiontCallDataSuccessFul.increment();
    }

    public void incrementApiSamples(int num) {
        this.counterSolarSamples.increment(num);
    }
}
