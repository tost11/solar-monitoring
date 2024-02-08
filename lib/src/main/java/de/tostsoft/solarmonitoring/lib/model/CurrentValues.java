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

    private Long lastSet;

    Float batteryVoltage;
    Float inputWatt;

    public boolean isUpToDate(){
        if(lastSet == null){
            return false;
        }
        var now = Instant.now();
        var last = Instant.ofEpochMilli(lastSet);
        Duration duration = Duration.between(now, last);
        return duration.abs().toMinutes() < 7.5;
    }


}
