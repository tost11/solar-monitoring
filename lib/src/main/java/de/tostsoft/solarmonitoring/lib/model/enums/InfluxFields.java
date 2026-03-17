package de.tostsoft.solarmonitoring.lib.model.enums;

public enum InfluxFields {

    calcProdKWHField("CalcProducedKWH"),
    calcByDevicesProdKWHField("CalcByDevicesProducedKWH"),
    //calcProdKWHDCField("CalcProducedKWHDC"),
    calcConsKWHField("CalcConsumedKWH"),
    calcByDevicesConsKWHField("CalcByDevicesConsumedKWH"),
    calcBatteryKWHField("CalcBatteryKWH"),
    calcByDevicesBatteryKWHField("CalcByDevicesBatteryKWH"),
    prodKWHField("ProducedKWH"),
    prodKWHDCField("ProducedKWHDC"),
    consKWHField("ConsumedKWH"),
    batteryKWHField("BatteryKWH"),
    gridConsumptionKWHField("GridConsumptionKWH"),
    gridFeedInKWHField("GridFeedInKWH"),
    calcGridConsumptionKWHField("CalcGridConsumptionKWH"),
    calcGridFeedInKWHField("CalcGridFeedInKWH"),
    calcByDevicesGridConsumptionKWHField("CalcByDevicesGridConsumptionKWH"),
    calcByDevicesGridFeedInKWHField("CalcByDevicesGridFeedInKWH"),
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
