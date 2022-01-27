package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SelfMadeSolarIfluxPoint extends GenericInfluxPoint {
    private float duration;
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

    //Consumption
    private Float consumptionVoltage;
    private Float consumptionAmpere;
    private Float consumptionWatt;

    //Consumption
    private Float consumptionInverterVoltage;
    private Float consumptionInverterAmpere;
    private Float consumptionInverterWatt;
    private Float inverterTemperature;

    private Float deviceTemperature;

    private Float totalConsumption;
}
