package de.tostsoft.solarmonitoring.model.enums;

public enum InfluxMeasurement {
  SELFMADE("selfmade-solar-data"),
  GRID("grid-solar-data"),
  SIMPLE("simple-solar-data"),
  GRID_INPUT("grid-solar-data-input"),
  GRID_OUTPUT("grid-solar-data-output");

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