package de.tostsoft.solarmonitoring.app.solarsystem;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.lib.model.ViewData;
import de.tostsoft.solarmonitoring.lib.model.enums.GraphFilter;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ViewDataDTOConverterTest {

    @Test
    public void testOwnerSeesAllFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, true);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            );
    }

    @Test
    public void testPublicViewerProductionModeRemovesConsumptionFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(1)
            .containsExactly(GraphFilter.INPUT_FREQUENCY);
    }

    @Test
    public void testPublicViewerAllModeKeepsAllFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.ALL, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.BATTERY_SOC,
                GraphFilter.GRID_WATT
            );
    }

    @Test
    public void testProductionModeWithNoConsumptionFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC
            );
    }

    @Test
    public void testProductionModeWithAllConsumptionFilters() {
        Set<GraphFilter> allConsumptionFilters = Set.of(
            GraphFilter.OUTPUT_WATT_DC,
            GraphFilter.OUTPUT_WATT_AC,
            GraphFilter.OUTPUT_WATT_COMBINED,
            GraphFilter.OUTPUT_VOLTAGE_DC,
            GraphFilter.OUTPUT_VOLTAGE_AC,
            GraphFilter.OUTPUT_AMPERE_DC,
            GraphFilter.OUTPUT_AMPERE_AC,
            GraphFilter.OUTPUT_FREQUENCY,
            GraphFilter.OUTPUT_TOTAL_CONSUMPTION,
            GraphFilter.BATTERY_WATT,
            GraphFilter.BATTERY_VOLTAGE,
            GraphFilter.BATTERY_AMPERE,
            GraphFilter.BATTERY_SOC,
            GraphFilter.GRID_WATT,
            GraphFilter.GRID_VOLTAGE,
            GraphFilter.GRID_AMPERE,
            GraphFilter.GRID_FREQUENCY,
            GraphFilter.MORE_TEMPERATURE
        );

        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(new HashSet<>(allConsumptionFilters))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void testProductionModeMixedFilters() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC,
                GraphFilter.INPUT_WATT_DC,
                GraphFilter.BATTERY_SOC,
                GraphFilter.BATTERY_VOLTAGE,
                GraphFilter.GRID_WATT,
                GraphFilter.OUTPUT_WATT_AC
            ))
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .hasSize(4)
            .containsExactlyInAnyOrder(
                GraphFilter.INPUT_FREQUENCY,
                GraphFilter.INPUT_VOLTAGE_DC,
                GraphFilter.INPUT_AMPERE_DC,
                GraphFilter.INPUT_WATT_DC
            );
    }

    @Test
    public void testNullGraphFilter() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(null)
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter()).isNull();
    }

    @Test
    public void testEmptyGraphFilter() {
        ViewData viewData = ViewData.builder()
            .batteryVoltage(48)
            .graphFilter(Set.of())
            .build();

        ViewDataDTO result = Converter.convertToViewDataDTO(viewData, PublicMode.PRODUCTION, false);

        Assertions.assertThat(result.getGraphFilter())
            .isNotNull()
            .isEmpty();
    }
}
