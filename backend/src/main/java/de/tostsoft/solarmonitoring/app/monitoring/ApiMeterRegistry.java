package de.tostsoft.solarmonitoring.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ApiMeterRegistry {

    private final Counter counterApiEndpiontCallData;
    private final Counter counterApiEndpiontCallDataSuccessFul;
    private final Counter counterSolarSamples;

    //client calls
    private final Counter counterApiClientCall;
    private final Counter counterApiClientCallSuccessFul;

    private final Counter counterApiClientLatest;
    private final Counter counterApiClientStatisticLatest;
    private final Counter counterApiClientAll;
    private final Counter counterApiClientStatisticAll;
    private final Counter counterApiClientMultAll;
    private final Counter counterApiClientMultStatisticAll;
    private final Counter counterApiClientMultLatest;
    private final Counter counterApiClientMultStatisticLatest;

    public ApiMeterRegistry(MeterRegistry registry) {
        this.counterApiEndpiontCallData = registry.counter("api.solar.calls");
        this.counterApiEndpiontCallDataSuccessFul = registry.counter("api.solar.calls.successful");
        this.counterSolarSamples = registry.counter("api.solar.samples");

        this.counterApiClientCall = registry.counter("api.client.calls");;
        this.counterApiClientCallSuccessFul = registry.counter("api.client.calls.successful");

        this.counterApiClientLatest = registry.counter("api.client.calls.latest");
        this.counterApiClientStatisticLatest = registry.counter("api.client.statistic.latest");
        this.counterApiClientAll = registry.counter("api.client.calls.all");
        this.counterApiClientStatisticAll = registry.counter("api.client.calls.statistic.all");
        this.counterApiClientMultAll = registry.counter("api.client.calls.mult.all");
        this.counterApiClientMultStatisticAll = registry.counter("api.client.calls.mult.statistic.all");
        this.counterApiClientMultLatest = registry.counter("api.client.calls.mult.latest");
        this.counterApiClientMultStatisticLatest = registry.counter("api.client.calls.mult.statistic.all");
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

    public void incrementApiClientCall() {
        this.counterApiClientCall.increment();
    }
    public void incrementApiClientCallSuccessFul() {
        this.counterApiClientCallSuccessFul.increment();
    }

    public void incrementApiClientCallLatest() {
        this.counterApiClientLatest.increment();
    }
    public void incrementApiClientCallStatisticLatest() {
        this.counterApiClientStatisticLatest.increment();
    }
    public void incrementApiClientCallAll() {
        this.counterApiClientAll.increment();
    }
    public void incrementApiClientCallStatisticAll() {
        this.counterApiClientStatisticAll.increment();
    }
    public void incrementApiClientCallMultAll() {
        this.counterApiClientMultAll.increment();
    }
    public void incrementApiClientCallMultStatisticAll() {
        this.counterApiClientMultStatisticAll.increment();
    }
    public void incrementApiClientCallMultLatest() {
        this.counterApiClientMultLatest.increment();
    }
    public void incrementApiClientCallMultStatisticLatest() {this.counterApiClientMultStatisticLatest.increment();}

}
