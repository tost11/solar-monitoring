package de.tostsoft.solarmonitoring.updater;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.SystemCurves;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
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

public class InfluxTaskServiceProductionCurveTest extends UpdaterBaseTest {

    @Autowired
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() throws InterruptedException {
        clearDatabase();
    }

    @Test
    public void testRunUpdateProductionCurvePopulatesLastTwoSlots() throws InterruptedException {
        Assumptions.assumeTrue(currentSlot1() >= 0, "Skipping: test runs in first 30 min of UTC");

        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys1");

        int slot1 = currentSlot1();
        int slot2 = slot1 + 1;

        writeInputWattPoints(user, system, slot1, 100.0, 200.0, 300.0);
        writeInputWattPoints(user, system, slot2, 500.0, 700.0);

        Thread.sleep(2000);

        influxTaskService.runUpdateProductionCurve(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getSystemCurves()).isNotNull();
        Assertions.assertThat(reloaded.getSystemCurves().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()).hasSize(96);
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()[slot1]).isEqualTo(200.0f);
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()[slot2]).isEqualTo(600.0f);
        Assertions.assertThat(reloaded.getSystemCurves().getProductionCurve()[0]).isNull();
    }

    @Test
    public void testRunUpdateProductionCurveWithNoData() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys2");

        influxTaskService.runUpdateProductionCurve(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        if (currentSlot1() < 0) {
            Assertions.assertThat(reloaded.getSystemCurves()).isNull();
            return;
        }
        Assertions.assertThat(reloaded.getSystemCurves()).isNotNull();
        Assertions.assertThat(reloaded.getSystemCurves().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        Float[] curve = reloaded.getSystemCurves().getProductionCurve();
        Assertions.assertThat(curve).hasSize(96);
        for (Float value : curve) {
            Assertions.assertThat(value).isNull();
        }
    }

    @Test
    public void testRunUpdateProductionCurvePreservesExistingSlots() throws InterruptedException {
        Assumptions.assumeTrue(currentSlot1() >= 0, "Skipping: test runs in first 30 min of UTC");

        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys3");

        int slot1 = currentSlot1();
        int slot2 = slot1 + 1;
        int existingSlot = 10;

        SystemCurves preExisting = SystemCurves.builder()
            .localDate(LocalDate.now(ZoneOffset.UTC))
            .productionCurve(new Float[96])
            .build();
        preExisting.getProductionCurve()[existingSlot] = 999.0f;
        solarSystemRepository.updateSystemCurves(system.getId(), preExisting);

        system = solarSystemRepository.findById(system.getId()).orElseThrow();

        writeInputWattPoints(user, system, slot1, 400.0);
        writeInputWattPoints(user, system, slot2, 800.0);

        Thread.sleep(2000);

        influxTaskService.runUpdateProductionCurve(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getSystemCurves()).isNotNull();
        Float[] curve = reloaded.getSystemCurves().getProductionCurve();
        Assertions.assertThat(curve[existingSlot]).isEqualTo(999.0f);
        Assertions.assertThat(curve[slot1]).isEqualTo(400.0f);
        Assertions.assertThat(curve[slot2]).isEqualTo(800.0f);
    }

    @Test
    public void testRunUpdateProductionCurveClearsOnNewDay() throws InterruptedException {
        Assumptions.assumeTrue(currentSlot1() >= 0, "Skipping: test runs in first 30 min of UTC");

        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys4");

        int slot1 = currentSlot1();
        int slot2 = slot1 + 1;

        SystemCurves yesterday = SystemCurves.builder()
            .localDate(LocalDate.now(ZoneOffset.UTC).minusDays(1))
            .productionCurve(new Float[96])
            .build();
        yesterday.getProductionCurve()[10] = 999.0f;
        yesterday.getProductionCurve()[20] = 888.0f;
        solarSystemRepository.updateSystemCurves(system.getId(), yesterday);

        system = solarSystemRepository.findById(system.getId()).orElseThrow();

        writeInputWattPoints(user, system, slot1, 150.0, 250.0);
        writeInputWattPoints(user, system, slot2, 300.0);

        Thread.sleep(2000);

        influxTaskService.runUpdateProductionCurve(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getSystemCurves()).isNotNull();
        Assertions.assertThat(reloaded.getSystemCurves().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        Float[] curve = reloaded.getSystemCurves().getProductionCurve();
        Assertions.assertThat(curve[10]).isNull();
        Assertions.assertThat(curve[20]).isNull();
        Assertions.assertThat(curve[slot1]).isEqualTo(200.0f);
        Assertions.assertThat(curve[slot2]).isEqualTo(300.0f);
    }

    private int currentSlot1() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentSlot = now.getHour() * 4 + now.getMinute() / 15;
        return currentSlot - 2;
    }

    private void writeInputWattPoints(User user, SolarSystem system, int slot, double... values) {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var slotStart = now.toLocalDate().atTime(slot / 4, (slot % 4) * 15).toInstant(ZoneOffset.UTC);
        for (int i = 0; i < values.length; i++) {
            var time = slotStart.plusSeconds(60L * (i + 1));
            writeInputWattPoint(user, system, time, values[i]);
        }
    }

    private void writeInputWattPoint(User user, SolarSystem system, Instant time, double inputWatt) {
        var point = Point.measurement(InfluxMeasurement.SOLAR_DATA.getName())
            .time(time.toEpochMilli(), WritePrecision.MS)
            .addField("InputWatt", inputWatt)
            .addTag("system", system.getInfluxTagName());
        influxConnection.writePointForUser(user.getInfluxBucketName(), point);
    }
}
