package de.tostsoft.solarmonitoring.app.solarsystem;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NamingsDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class SolarSystemControllerTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Value("${system.defaultMaxSamplesDay}")
    private long defaultMaxSamplesDaySysgtem;

    private RegisterSolarSystemDTO crateDefaultRegisterDTO(){
        var systemDTO = new RegisterSolarSystemDTO();

        systemDTO.setName("test");
        systemDTO.setType(SolarSystemType.GRID);
        systemDTO.setPublicMode(PublicMode.NONE);
        systemDTO.setCalculateCombinedValuesAfterwards(false);
        systemDTO.setTimezone("UTC");

        systemDTO.setViewData(new ViewDataDTO());
        systemDTO.getViewData().setShowAmpere(false);

        systemDTO.setNamings(new NamingsDTO());
        systemDTO.getNamings().setBatteries(new HashMap<>());
        systemDTO.getNamings().setDevices(new HashMap<>());
        systemDTO.getNamings().setInputsAC(new HashMap<>());
        systemDTO.getNamings().setOutputsAC(new HashMap<>());
        systemDTO.getNamings().setInputsDC(new HashMap<>());
        systemDTO.getNamings().setOutputsDC(new HashMap<>());
        systemDTO.getNamings().setGrids(new HashMap<>());

        return systemDTO;
    }

    @Test
    public void checkAdminSystemUnlimitedSamples(){
        addUser(true);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);
        Assertions.assertThat(system.getMaxSamplesOnDay()).isEqualTo(-1);
    }

    @Test
    public void checkNotAdminSystemLimitedSamples(){
        addUser(false);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);
        Assertions.assertThat(system.getMaxSamplesOnDay()).isEqualTo(defaultMaxSamplesDaySysgtem);
    }

    @Test
    public void checkCreationCorrect(){
        addUser(true);
        var jwt = signIn();

        var systemDTO = crateDefaultRegisterDTO();
        systemDTO.setName("Test");
        systemDTO.setType(SolarSystemType.GRID);
        systemDTO.setPublicMode(PublicMode.NONE);
        systemDTO.setBuildingDate(ZonedDateTime.now());
        systemDTO.setTimezone("UTC");
        systemDTO.setCalculateCombinedValuesAfterwards(false);
        systemDTO.setDeyeSunSerialNumbers("123456789");
        systemDTO.setElectricityPrice(0.33f);
        systemDTO.setElectricityPriceFeedIn(0.66f);
        systemDTO.setShortener("tes");
        systemDTO.getViewData().setBatteryVoltage(12);
        systemDTO.getViewData().setMaxSolarVoltage(60);
        systemDTO.getViewData().setVoltageAC(230);
        systemDTO.getViewData().setDefaultDelay(300);
        systemDTO.getViewData().setHasACInput(true);
        systemDTO.getViewData().setShowAmpere(true);
        systemDTO.getViewData().setHasDCOutput(true);
        systemDTO.getViewData().setHasTemperature(true);
        systemDTO.getViewData().setHideTotalConsumption(true);
        systemDTO.getViewData().setIsBatteryPercentage(true);
        systemDTO.getViewData().setTotalPricingPublicOverride(true);
        systemDTO.getViewData().setProductionForTotalPricing(true);
        systemDTO.getViewData().setTotalFilter(Set.of("CalcProducedKWH", "CalcByDevicesConsumedKWH"));

        doRestRequest("api/system",systemDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var system = solarSystemRepository.findAll().get(0);

        Assertions.assertThat(system.getName()).isEqualTo("test");
        Assertions.assertThat(system.getViewName()).isEqualTo("Test");
        Assertions.assertThat(system.getType()).isEqualTo(SolarSystemType.GRID);
        Assertions.assertThat(system.getPublicMode()).isEqualTo(PublicMode.NONE);
        Assertions.assertThat(system.getTimezone()).isEqualTo("UTC");
        Assertions.assertThat(system.getCalculateCombinedValuesAfterwards()).isFalse();
        Assertions.assertThat(system.getDeyeSunSerials()).contains(123456789L);
        Assertions.assertThat(system.getElectricityPrice()).isEqualTo(0.33f);
        Assertions.assertThat(system.getElectricityPriceFeedIn()).isEqualTo(0.66f);
        Assertions.assertThat(system.getShortener()).isEqualTo("tes");
        Assertions.assertThat(system.getViewData().getBatteryVoltage()).isEqualTo(12);
        Assertions.assertThat(system.getViewData().getMaxSolarVoltage()).isEqualTo(60);
        Assertions.assertThat(system.getViewData().getVoltageAC()).isEqualTo(230);
        Assertions.assertThat(system.getViewData().getDefaultDelay()).isEqualTo(300);
        Assertions.assertThat(system.getViewData().getHasACInput()).isTrue();
        Assertions.assertThat(system.getViewData().getShowAmpere()).isTrue();
        Assertions.assertThat(system.getViewData().getHasDCOutput()).isTrue();
        Assertions.assertThat(system.getViewData().getHasTemperature()).isTrue();
        Assertions.assertThat(system.getViewData().getHideTotalConsumption()).isTrue();
        Assertions.assertThat(system.getViewData().getIsBatteryPercentage()).isTrue();
        Assertions.assertThat(system.getViewData().getTotalPricingPublicOverride()).isTrue();
        Assertions.assertThat(system.getViewData().getProductionForTotalPricing()).isTrue();
        Assertions.assertThat(system.getViewData().getTotalFilter())
            .containsExactlyInAnyOrder("CalcProducedKWH", "CalcByDevicesConsumedKWH");
    }

}
