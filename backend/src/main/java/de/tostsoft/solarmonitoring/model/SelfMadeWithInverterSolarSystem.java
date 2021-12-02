package de.tostsoft.solarmonitoring.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SelfMadeWithInverterSolarSystem extends GenericInfluxPoint {
    private long timestamp;
    //Solar panel
    private float chargeVolt;
    private float chargeAmpere;
    private Float chargeWatt;
    private Float chargeTemperature;
    //Battery
    private Float batteryVoltage;
    private Float batteryAmpere;
    private Float batteryWatt;
    private Float batteryPercentage;
    private Float batteryTemperature;
    //
    private Float consumptionDcVoltage;
    private Float consumptionDcAmpere;
    private Float consumptionDcWatt;
    private Float outputAcVoltage;
    private Float outputAcAmpere;
    private Float outputAcWatt;

    private Float deviceTemperature;
    private Float inverterTemperature;

    private Float totalConsumption;

}
