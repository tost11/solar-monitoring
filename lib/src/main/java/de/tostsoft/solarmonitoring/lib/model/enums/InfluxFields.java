package de.tostsoft.solarmonitoring.lib.model.enums;

public enum InfluxFields {

    calcProdKWHField("CalcProducedKWH"),
    calcProdKWHDCField("CalcProducedKWHDC"),
    calcConsKWHField("CalcConsumedKWH"),
    calcBatteryKWHField("CalcBatteryKWH"),
    prodKWHField("ProducedKWH"),
    prodKWHDCField("ProducedKWHDC"),
    consKWHField("ConsumedKWH"),
    batteryKWHField("BatteryKWH"),
    energyPriceMeasurement("energyPrice");

    private final String name;

    private InfluxFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

}
