package de.tostsoft.solarmonitoring.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SelfMadeSolarSystem extends GenericInfluxPoint {
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

    private Float deviceTemperature;

    private Float totalConsumption;

    public SelfMadeSolarSystem(long timeStep,float chargeVolt,float chargeAmpere) {
        super(timeStep, InfliuxSolarMeasurement.SELFMADE);
        this.chargeVolt = chargeVolt;
        this.chargeAmpere = chargeAmpere;
        this.chargeWatt = chargeVolt*chargeAmpere;

    }

    public SelfMadeSolarSystem(long timeStep, float chargeVolt, float chargeAmpere, Float chargeWatt, Float chargeTemperature, Float batteryVoltage, Float batteryAmpere, Float batteryWatt, Float batteryPercentage, Float batteryTemperature, Float consumptionDcVoltage, Float consumptionDcAmpere, Float consumptionDcWatt, Float deviceTemperature, Float totalConsumption) {
        super(timeStep, InfliuxSolarMeasurement.SELFMADE);
        this.chargeVolt = chargeVolt;
        this.chargeAmpere = chargeAmpere;
        this.chargeWatt = chargeWatt;
        this.chargeTemperature = chargeTemperature;
        this.batteryVoltage = batteryVoltage;
        this.batteryAmpere = batteryAmpere;
        this.batteryWatt = batteryWatt;
        this.batteryPercentage = batteryPercentage;
        this.batteryTemperature = batteryTemperature;
        this.consumptionDcVoltage = consumptionDcVoltage;
        this.consumptionDcAmpere = consumptionDcAmpere;
        this.consumptionDcWatt = consumptionDcWatt;
        this.deviceTemperature = deviceTemperature;
        this.totalConsumption = totalConsumption;
    }

}
