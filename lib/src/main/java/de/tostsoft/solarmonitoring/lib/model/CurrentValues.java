package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentValues {

    public static final float ONLINE_CHECK_TOLERANCE = 1.3f;

    private Long lastSet;

    Float batteryVoltage;
    Float inputWatt;

    public boolean isUpToDate(Duration durToTest){
        if(lastSet == null){
            return false;
        }
        if(durToTest == null){
            durToTest = Duration.ofSeconds(60);
        }
        var now = Instant.now();
        var last = Instant.ofEpochMilli(lastSet);
        Duration duration = Duration.between(now, last);
        return duration.abs().toSeconds() < durToTest.toSeconds() * ONLINE_CHECK_TOLERANCE;
    }
}
