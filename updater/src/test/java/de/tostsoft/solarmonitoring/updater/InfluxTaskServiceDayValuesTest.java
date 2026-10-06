package de.tostsoft.solarmonitoring.updater;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxFields;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

public class InfluxTaskServiceDayValuesTest extends UpdaterBaseTest {

    @Autowired
    private InfluxTaskService influxTaskService;

    @BeforeEach
    public void prepare() throws InterruptedException {
        clearDatabase();
    }

    @Test
    public void testRunUpdateDayValuesReadsDayDataFromInflux() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys1");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant midnight = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        // two points with different cumulative values, max must win
        // a few seconds in the past so the point stays inside the second-precision query stop
        writeDayPoint(user, system, midnight, 10.0, 5.0);
        writeDayPoint(user, system, Instant.now().minusSeconds(5), 15.0, 8.0);

        influxTaskService.runUpdateDayValues(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getDayValues()).isNotNull();
        Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isEqualTo(15f);
        Assertions.assertThat(reloaded.getDayValues().getConsumedKWH()).isEqualTo(8f);
        Assertions.assertThat(reloaded.getDayValues().getGridConsumedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getGridFeedInKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getLocalDate()).isEqualTo(today);
    }

    @Test
    public void testRunUpdateDayValuesRespectsRawFieldPriority() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys2");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant midnight = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        // raw field exists alongside calc-by-devices field, raw one must win
        var point = Point.measurement(InfluxMeasurement.SOLAR_DAY_DATA.getName())
            .time(midnight.toEpochMilli(), WritePrecision.MS)
            .addField(InfluxFields.prodKWHField.getName(), 20.0)
            .addField(InfluxFields.calcByDevicesProdKWHField.getName(), 15.0)
            .addField(InfluxFields.consKWHField.getName(), 9.0)
            .addTag("system", system.getInfluxTagName());
        influxConnection.writePointForUser(user.getInfluxBucketName(), point);

        influxTaskService.runUpdateDayValues(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getDayValues()).isNotNull();
        Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isEqualTo(20f);
        Assertions.assertThat(reloaded.getDayValues().getConsumedKWH()).isEqualTo(9f);
    }

    @Test
    public void testRunUpdateDayValuesRespectsTotalFilter() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys4");

        // blacklist the raw field, only the calc-by-devices field may be used
        system.getViewData().setTotalFilter(Set.of(InfluxFields.prodKWHField.getName()));
        system = solarSystemRepository.save(system);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant midnight = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        // raw field exists in Influx but is filtered out of the query
        var point = Point.measurement(InfluxMeasurement.SOLAR_DAY_DATA.getName())
            .time(midnight.toEpochMilli(), WritePrecision.MS)
            .addField(InfluxFields.prodKWHField.getName(), 20.0)
            .addField(InfluxFields.calcByDevicesProdKWHField.getName(), 15.0)
            .addTag("system", system.getInfluxTagName());
        influxConnection.writePointForUser(user.getInfluxBucketName(), point);

        influxTaskService.runUpdateDayValues(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getDayValues()).isNotNull();
        Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isEqualTo(15f);
    }

    @Test
    public void testRunUpdateDayValuesWithAllFieldsFiltered() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys5");

        // blacklist every day value field
        system.getViewData().setTotalFilter(Set.of(
            InfluxFields.prodKWHField.getName(),
            InfluxFields.calcByDevicesProdKWHField.getName(),
            InfluxFields.calcProdKWHField.getName(),
            InfluxFields.consKWHField.getName(),
            InfluxFields.calcByDevicesConsKWHField.getName(),
            InfluxFields.calcConsKWHField.getName(),
            InfluxFields.gridConsKWHField.getName(),
            InfluxFields.calcByDevicesGridConsKWHField.getName(),
            InfluxFields.calcGridConsKWHField.getName(),
            InfluxFields.gridFeedInKWHField.getName(),
            InfluxFields.calcByDevicesGridFeedInKWHField.getName(),
            InfluxFields.calcGridFeedInKWHField.getName()
        ));
        system = solarSystemRepository.save(system);

        influxTaskService.runUpdateDayValues(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getDayValues()).isNotNull();
        Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getConsumedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getGridConsumedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getGridFeedInKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    public void testRunUpdateDayValuesWithoutData() {
        User user = addUser(true, "owner");
        SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys3");

        influxTaskService.runUpdateDayValues(system);

        SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getDayValues()).isNotNull();
        Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getConsumedKWH()).isNull();
        Assertions.assertThat(reloaded.getDayValues().getLocalDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
  }

  @Test
  public void testRunUpdateDayValuesStoresPricesAndCalcValues() {
      User user = addUser(true, "owner");
      SolarSystem system = addSolarSystemForUser(user, SolarSystemType.GRID, "sys6");

      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      Instant midnight = today.atStartOfDay(ZoneOffset.UTC).toInstant();

      var point = Point.measurement(InfluxMeasurement.SOLAR_DAY_DATA.getName())
          .time(midnight.toEpochMilli(), WritePrecision.MS)
          .addField(InfluxFields.prodKWHField.getName(), 100.0)
          .addField(InfluxFields.consKWHField.getName(), 50.0)
          .addField(InfluxFields.gridConsKWHField.getName(), 10.0)
          .addField(InfluxFields.gridFeedInKWHField.getName(), 20.0)
          .addField(InfluxFields.prodKWHField.getName() + "Price", 30.0)
          .addField(InfluxFields.consKWHField.getName() + "Price", 25.0)
          .addField(InfluxFields.gridConsKWHField.getName() + "Price", 5.0)
          .addField(InfluxFields.gridFeedInKWHField.getName() + "Price", 7.0)
          .addField(InfluxFields.gridFeedInKWHField.getName() + "Price2", 9.0)
          .addTag("system", system.getInfluxTagName());
      influxConnection.writePointForUser(user.getInfluxBucketName(), point);

      influxTaskService.runUpdateDayValues(system);

      SolarSystem reloaded = solarSystemRepository.findById(system.getId()).orElseThrow();
      Assertions.assertThat(reloaded.getDayValues()).isNotNull();
      Assertions.assertThat(reloaded.getDayValues().getProducedKWH()).isEqualTo(100f);
      Assertions.assertThat(reloaded.getDayValues().getConsumedKWH()).isEqualTo(50f);
      Assertions.assertThat(reloaded.getDayValues().getGridConsumedKWH()).isEqualTo(10f);
      Assertions.assertThat(reloaded.getDayValues().getGridFeedInKWH()).isEqualTo(20f);
      Assertions.assertThat(reloaded.getDayValues().getProducedKWHPrice()).isEqualTo(30f);
      Assertions.assertThat(reloaded.getDayValues().getConsumedKWHPrice()).isEqualTo(25f);
      Assertions.assertThat(reloaded.getDayValues().getGridConsumedKWHPrice()).isEqualTo(5f);
      // stored gridFeedInKWHPrice comes from the Price2 field (9), not the plain Price field (7)
      Assertions.assertThat(reloaded.getDayValues().getGridFeedInKWHPrice()).isEqualTo(9f);
      // calcConsumedKWH = max(0, 50 - 20) + 10 = 40
      Assertions.assertThat(reloaded.getDayValues().getCalcConsumedKWH()).isEqualTo(40f);
      // calcConsumedKWHPrice = max(0, 25 - 7) = 18
      Assertions.assertThat(reloaded.getDayValues().getCalcConsumedKWHPrice()).isEqualTo(18f);
  }

  private void writeDayPoint(User user, SolarSystem system, Instant time, double produced, double consumed) {
        var point = Point.measurement(InfluxMeasurement.SOLAR_DAY_DATA.getName())
            .time(time.toEpochMilli(), WritePrecision.MS)
            .addField(InfluxFields.calcByDevicesProdKWHField.getName(), produced)
            .addField(InfluxFields.calcByDevicesConsKWHField.getName(), consumed)
            .addTag("system", system.getInfluxTagName());
        influxConnection.writePointForUser(user.getInfluxBucketName(), point);
    }
}
