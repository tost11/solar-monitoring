package de.tostsoft.solarmonitoring.lib.model.enums;

public enum GraphFilter {
    // Input accordion filters
    INPUT_WATT_DC,
    INPUT_WATT_AC,
    INPUT_VOLTAGE_DC,
    INPUT_VOLTAGE_AC,
    INPUT_AMPERE_DC,
    INPUT_AMPERE_AC,
    INPUT_FREQUENCY,

    // Output accordion filters
    OUTPUT_WATT_DC,
    OUTPUT_WATT_AC,
    OUTPUT_VOLTAGE_DC,
    OUTPUT_VOLTAGE_AC,
    OUTPUT_AMPERE_DC,
    OUTPUT_AMPERE_AC,
    OUTPUT_FREQUENCY,
    OUTPUT_TOTAL_CONSUMPTION,

    // Battery accordion filters
    BATTERY_WATT,
    BATTERY_VOLTAGE,
    BATTERY_AMPERE,
    BATTERY_SOC,

    // Grid accordion filters
    GRID_WATT,
    GRID_VOLTAGE,
    GRID_AMPERE,
    GRID_FREQUENCY,

    // More accordion filters
    MORE_TEMPERATURE
}
