package de.tostsoft.solarmonitoring.model.enums;

public enum InfluxMeasurement {
  SOLAR_DAY_DATA("day-solar-data"),
  SOLAR_DATA("solar-data"),
  SOLAR_DATA_DEVICE("device-solar-data"),
  SOLAR_DATA_INPUT_DC("input-solar-dc-data"),
  SOLAR_DATA_OUTPUT_DC("output-solar-dc-data"),
  SOLAR_DATA_INPUT_AC("input-solar-ad-data"),
  SOLAR_DATA_OUTPUT_AC("output-solar-ac-data"),
  SOLAR_DATA_BATTERY("battery-solar-data");

  private final String name;

  /**
   * @param name
   */
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