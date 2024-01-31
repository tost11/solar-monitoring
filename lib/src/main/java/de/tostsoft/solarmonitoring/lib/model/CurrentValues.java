package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentValues {

    private Long lastSet;

    Float batteryVoltage;
    Float inputWatt;
}
