package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class CurrentValuesDTO {
    Float inputWatt;
    Float outputWatt;
    Float gridWatt;
    Float batteryVoltage;
    Float batteryPercentage;
    Float batteryWatt;
}
