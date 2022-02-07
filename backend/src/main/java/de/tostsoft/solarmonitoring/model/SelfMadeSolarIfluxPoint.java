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
    private Float consumptionDeviceVoltage;
    private Float consumptionDeviceAmpere;
    private Float consumptionDeviceWatt;

    //Consumption
    private Float consumptionInverterVoltage;
    private Float consumptionInverterAmpere;
    private Float consumptionInverterWatt;
    private Float inverterTemperature;

    private Float deviceTemperature;

    private Float totalConsumption;

    private void copyTo(SelfMadeSolarIfluxPoint ret){
        super.copyTo(ret);
        ret.duration = duration;
        ret.chargeVolt = chargeVolt;
        ret.chargeAmpere = chargeAmpere;
        ret.chargeWatt = chargeWatt;
        ret.chargeTemperature = chargeTemperature;
        ret.batteryVoltage = batteryVoltage;
        ret.batteryAmpere = batteryAmpere;
        ret.batteryWatt = batteryWatt;
        ret.batteryPercentage = batteryPercentage;
        ret.batteryTemperature = batteryTemperature;
        ret.consumptionDeviceVoltage = consumptionDeviceVoltage;
        ret.consumptionDeviceAmpere = consumptionDeviceAmpere;
        ret.consumptionDeviceWatt = consumptionDeviceWatt;
        ret.consumptionInverterVoltage = consumptionInverterVoltage;
        ret.consumptionInverterAmpere = consumptionInverterAmpere;
        ret.consumptionInverterWatt = consumptionInverterWatt;
        ret.deviceTemperature = deviceTemperature;
        ret.totalConsumption = totalConsumption;
    }

    public SelfMadeSolarIfluxPoint copy(){
        SelfMadeSolarIfluxPoint ret = new SelfMadeSolarIfluxPoint();
        copyTo(ret);
        return ret;
    }

}
