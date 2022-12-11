package de.tostsoft.solarmonitoring.model.enums;

public enum InfluxMeasurement {
  SOLAR_DAY_DATA("day-solar-data"),
  SOLAR_DATA("solar-data"),
  SOLAR_DATA_DEVICE("solar-data"),
  SOLAR_DATA_INPUT("device-solar-data"),
  SOLAR_DATA_OUTPUT("output-solar-data"),
  SOLAR_DATA_BATTERY("input-solar-data");


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