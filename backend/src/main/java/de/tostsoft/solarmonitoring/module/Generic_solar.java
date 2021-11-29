package de.tostsoft.solarmonitoring.module;

import lombok.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Generic_solar {
    private float solarVoltage;
    private float solarAmpere;
    private float solarWatt;
    private Float batteryVoltage;
    private Float batteryAmpere;
    private Float batteryWatt;
    private Float outputDcVoltage;
    private Float outputDcAmpere;
    private Float outputDcWatt;
    private Float outputAcVoltage;
    private Float outputAcAmpere;
    private Float outputAcWatt;
    private float temperatureLoader;
    private float temperatureBattery;
    private float temperatureCustom;
    private float totalConsumption;
    private long timestamp;
    private Float batteryPercentage;
}
