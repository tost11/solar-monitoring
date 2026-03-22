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
    gridConsKWHField("GridConsumedKWH"),
    gridFeedInKWHField("GridFeedInKWH"),
    calcGridConsKWHField("CalcGridConsumedKWH"),
    calcGridFeedInKWHField("CalcGridFeedInKWH"),
    calcByDevicesGridConsKWHField("CalcByDevicesGridConsumedKWH"),
    calcByDevicesGridFeedInKWHField("CalcByDevicesGridFeedInKWH"),
    energyPriceMeasurement("energyPrice"),
    energyPriceFeedInMeasurement("energyPriceFeedIn");

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
