package de.tostsoft.solarmonitoring.lib.model.enums;

public enum InfluxMeasurement {
  SOLAR_DAY_DATA_DEVICE("device-day-solar-data"),
  SOLAR_DAY_DATA("day-solar-data"),
  SOLAR_DATA("solar-data"),
  SOLAR_DATA_DEVICE("device-solar-data"),
  SOLAR_DATA_INPUT_DC("input-solar-dc-data"),
  SOLAR_DATA_OUTPUT_DC("output-solar-dc-data"),
  SOLAR_DATA_INPUT_AC("input-solar-ac-data"),
  SOLAR_DATA_OUTPUT_AC("output-solar-ac-data"),
  SOLAR_DATA_BATTERY("battery-solar-data"),
  CUSTOM_STATUS_BOOLEAN("custom-status-boolean"),
  SELDOM_CHANGING_STATS("seldom-changing-stats");

  private final String name;

  private InfluxMeasurement(final String name) {
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