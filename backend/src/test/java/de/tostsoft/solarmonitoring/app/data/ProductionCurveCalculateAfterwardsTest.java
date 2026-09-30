package de.tostsoft.solarmonitoring.app.data;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataController;
import de.tostsoft.solarmonitoring.app.controller.SolarDataConverter;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.InputDCDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.OutputACDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductionCurveCalculateAfterwardsTest extends AppBaseTest {

    @Autowired
    private SolarDataController solarDataController;

    @Autowired
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testProductionCurveWithCalculateAfterwards() throws InterruptedException {
        Assumptions.assumeTrue(currentSlot1() >= 0, "Skipping: test runs in first 30 min of UTC");

        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system = solarSystemRepository.save(system);

        int slot1 = currentSlot1();
        int slot2 = slot1 + 1;

        var slot2Mid = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
            .atTime(slot2 / 4, (slot2 % 4) * 15 + 5)
            .toInstant(ZoneOffset.UTC);

        SampleDTO sampleDTO = createTwoDeviceSample(3000f, 2000f);
        sampleDTO.setDuration(300f);
        sampleDTO.setTimestamp(slot2Mid.toEpochMilli());

        solarDataController.PostDeviceMult(system.getId(), Collections.singletonList(sampleDTO), "token");

        Thread.sleep(SolarDataConverter.AFTERWARDS_CALCULATION_WAIT * 1000 + 5000);

        system = solarSystemRepository.findById(system.getId()).orElseThrow();
        influxTaskService.runUpdateProductionCurve(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getSystemCurves()).isNotNull();
        Assertions.assertThat(reloaded.getSystemCurves().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()).hasSize(96);
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()[slot2]).isEqualTo(5000.0f);
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()[slot1]).isNull();
    }

    private SampleDTO createTwoDeviceSample(float device1Watt, float device2Watt) {
        SampleDTO sampleDTO = new SampleDTO();
        List<DeviceDTO> devices = new ArrayList<>();

        DeviceDTO device1 = new DeviceDTO();
        device1.setId(0L);
        List<InputDCDTO> inputDC1 = new ArrayList<>();
        var dc1 = new InputDCDTO();
        dc1.setId(0L);
        dc1.setWatt(device1Watt);
        inputDC1.add(dc1);
        device1.setInputsDC(inputDC1);
        List<OutputACDTO> outputAC1 = new ArrayList<>();
        var ac1 = new OutputACDTO();
        ac1.setId(0L);
        outputAC1.add(ac1);
        device1.setOutputsAC(outputAC1);
        devices.add(device1);

        DeviceDTO device2 = new DeviceDTO();
        device2.setId(1L);
        List<InputDCDTO> inputDC2 = new ArrayList<>();
        var dc2 = new InputDCDTO();
        dc2.setId(0L);
        dc2.setWatt(device2Watt);
        inputDC2.add(dc2);
        device2.setInputsDC(inputDC2);
        List<OutputACDTO> outputAC2 = new ArrayList<>();
        var ac2 = new OutputACDTO();
        ac2.setId(0L);
        outputAC2.add(ac2);
        device2.setOutputsAC(outputAC2);
        devices.add(device2);

        sampleDTO.setDevices(devices);
        return sampleDTO;
    }

    private int currentSlot1() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentSlot = now.getHour() * 4 + now.getMinute() / 15;
        return currentSlot - 2;
    }
}
