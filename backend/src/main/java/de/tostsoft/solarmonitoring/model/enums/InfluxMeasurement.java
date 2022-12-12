package de.tostsoft.solarmonitoring.model.enums;

import java.util.HashMap;

public enum InfluxMeasurement {
  SOLAR_DAY_DATA("day-solar-data"),
  SOLAR_DATA("solar-data"),
  SOLAR_DATA_DEVICE("device-solar-data"),
  SOLAR_DATA_INPUT("input-solar-data"),
  SOLAR_DATA_OUTPUT("output-solar-data"),
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