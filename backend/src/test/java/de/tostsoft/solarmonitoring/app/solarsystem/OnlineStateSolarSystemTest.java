package de.tostsoft.solarmonitoring.app.solarsystem;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataController;
import de.tostsoft.solarmonitoring.app.controller.SolarSystemController;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemSearchDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.BatteryDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.GridDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.InputDCDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.OutputDCDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.ViewData;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class OnlineStateSolarSystemTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Autowired
    private SolarDataController solarDataController;

    @Autowired
    private SolarSystemController solarSystemController;

    private SampleDTO createSample(){
        SampleDTO sampleDTO = new SampleDTO();

        List<DeviceDTO> deviceDTOList = new ArrayList<>();
        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTOList.add(deviceDTO);
        sampleDTO.setDevices(deviceDTOList);
        InputDCDTO inputDCDTO = new InputDCDTO();
        List<InputDCDTO> inputDCDTOList = new ArrayList<>();
        inputDCDTOList.add(inputDCDTO);
        deviceDTO.setInputsDC(inputDCDTOList);
        BatteryDTO batteryDTO = new BatteryDTO();
        List<BatteryDTO> batteryDTOList = new ArrayList<>();
        batteryDTOList.add(batteryDTO);
        deviceDTO.setBatteries(batteryDTOList);
        OutputDCDTO outputDCDTO = new OutputDCDTO();
        List<OutputDCDTO> outputDCDTOList = new ArrayList<>();
        outputDCDTOList.add(outputDCDTO);
        deviceDTO.setOutputsDC(outputDCDTOList);
        GridDTO gridDTO = new GridDTO();
        List<GridDTO> gridDTOList = new ArrayList<>();
        gridDTOList.add(gridDTO);
        deviceDTO.setGrids(gridDTOList);

        deviceDTO.setId(0L);
        inputDCDTO.setId(0L);
        batteryDTO.setId(0L);
        outputDCDTO.setId(0L);
        gridDTO.setId(0L);

        return sampleDTO;
    }

    private void assertValues(float inputWatt, float outputWatt, float gridWatt, float batteryVoltage, float batteryPercentage, float batteryWatt){
        var params = new SolarSystemSearchDTO();
        params.setIsPublic(true);

        var pagedResponse = solarSystemController.search(params);
        var systems = pagedResponse.getContent();

        Assertions.assertThat(systems).hasSize(1);
        var sys = systems.get(0);
        Assertions.assertThat(sys.getCurrentValues()).isNotNull();
        var viewData = sys.getCurrentValues();
        Assertions.assertThat(viewData.getInputWatt()).isEqualTo(inputWatt);
        Assertions.assertThat(viewData.getOutputWatt()).isEqualTo(outputWatt);
        Assertions.assertThat(viewData.getGridWatt()).isEqualTo(gridWatt);
        Assertions.assertThat(viewData.getBatteryVoltage()).isEqualTo(batteryVoltage);
        Assertions.assertThat(viewData.getBatteryPercentage()).isEqualTo(batteryPercentage);
        Assertions.assertThat(viewData.getBatteryWatt()).isEqualTo(batteryWatt);
    }

    private void assertOffline(){
        var params = new SolarSystemSearchDTO();
        params.setIsPublic(true);

        var pagedResponse = solarSystemController.search(params);
        var systems = pagedResponse.getContent();

        Assertions.assertThat(systems).hasSize(1);
        var sys = systems.get(0);
        Assertions.assertThat(sys.getCurrentValues()).isNull();
    }


    @Test
    public void checkOlineStateShown(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);
        var outputDCDTO = sampleDTO.getDevices().get(0).getOutputsDC().get(0);
        var gridDTO = sampleDTO.getDevices().get(0).getGrids().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        batteryDTO.setWatt(15.f);
        batteryDTO.setId(0L);
        sampleDTO.getDevices().get(0).setBatteryPercentage(75.f);
        outputDCDTO.setWatt(8.f);
        gridDTO.setWatt(5.f);
        sampleDTO.setDuration(30.f);

        sampleDTO.setTimestamp(start.toEpochMilli());

        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertValues(10, 8, 5, 12, 75, 15);
    }

    @Test
    public void checkOlineStateOrderCorrect(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);
        var outputDCDTO = sampleDTO.getDevices().get(0).getOutputsDC().get(0);
        var gridDTO = sampleDTO.getDevices().get(0).getGrids().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        batteryDTO.setWatt(15.f);
        sampleDTO.getDevices().get(0).setBatteryPercentage(70.f);
        outputDCDTO.setWatt(8.f);
        gridDTO.setWatt(5.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus(5,ChronoUnit.SECONDS).toEpochMilli());
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        inputDCDTO.setWatt(20.f);
        batteryDTO.setVoltage(24.f);
        batteryDTO.setWatt(30.f);
        sampleDTO.getDevices().get(0).setBatteryPercentage(80.f);
        outputDCDTO.setWatt(16.f);
        gridDTO.setWatt(10.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.toEpochMilli());
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertValues(20, 16, 10, 24, 80, 30);

        inputDCDTO.setWatt(40.f);
        batteryDTO.setVoltage(48.f);
        batteryDTO.setWatt(60.f);
        sampleDTO.getDevices().get(0).setBatteryPercentage(90.f);
        outputDCDTO.setWatt(32.f);
        gridDTO.setWatt(20.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus(10,ChronoUnit.SECONDS).toEpochMilli());
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertValues(20, 16, 10, 24, 80, 30);
    }

    @Test
    public void checkOnlineStateDefaultTimeout(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus(90,ChronoUnit.SECONDS).toEpochMilli());
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertOffline();
    }

    @Test
    public void checkOnlineStateDefaultTimeoutTolerance(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);
        var outputDCDTO = sampleDTO.getDevices().get(0).getOutputsDC().get(0);
        var gridDTO = sampleDTO.getDevices().get(0).getGrids().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        batteryDTO.setWatt(15.f);
        sampleDTO.getDevices().get(0).setBatteryPercentage(75.f);
        outputDCDTO.setWatt(8.f);
        gridDTO.setWatt(5.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus(65,ChronoUnit.SECONDS).toEpochMilli()); //70 seconds, default is 60 and tolerance * 1.2 so it should work
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertValues(10, 8, 5, 12, 75, 15);
    }

    @Test
    public void checkOnlineStateSetTimeout(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .defaultDelay(300)
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus((int)(system.getViewData().getDefaultDelay() * (CurrentValues.ONLINE_CHECK_TOLERANCE + 0.1)),ChronoUnit.SECONDS).toEpochMilli()); // 1.3 is tolerance so 1.4 should fail
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertOffline();
    }

    @Test
    public void checkOnlineStateSetTimeoutTolerance(){
        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID_BATTERY);
        system.setPublicMode(PublicMode.ALL);
        system.setViewData(ViewData.builder()
                .defaultDelay(300)
                .build());
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createSample();
        var inputDCDTO = sampleDTO.getDevices().get(0).getInputsDC().get(0);
        var batteryDTO = sampleDTO.getDevices().get(0).getBatteries().get(0);
        var outputDCDTO = sampleDTO.getDevices().get(0).getOutputsDC().get(0);
        var gridDTO = sampleDTO.getDevices().get(0).getGrids().get(0);

        inputDCDTO.setWatt(10.f);
        batteryDTO.setVoltage(12.f);
        batteryDTO.setWatt(15.f);
        sampleDTO.getDevices().get(0).setBatteryPercentage(75.f);
        outputDCDTO.setWatt(8.f);
        gridDTO.setWatt(5.f);
        sampleDTO.setDuration(30.f);
        sampleDTO.setTimestamp(start.minus((int)(system.getViewData().getDefaultDelay() * (CurrentValues.ONLINE_CHECK_TOLERANCE - 0.1)),ChronoUnit.SECONDS).toEpochMilli());  // 1.3 is tolerance so 1.2 should fail
        solarDataController.PostDevice(system.getId(),sampleDTO,"token");

        assertValues(10, 8, 5, 12, 75, 15);
    }
}
