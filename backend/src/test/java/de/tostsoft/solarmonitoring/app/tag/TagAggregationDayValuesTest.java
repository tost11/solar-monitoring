package de.tostsoft.solarmonitoring.app.tag;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.dto.SystemContributionDTO;
import de.tostsoft.solarmonitoring.lib.dto.TagAggregationDTO;
import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.DayValues;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;

public class TagAggregationDayValuesTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    private SolarSystem createSystemWithTag(User owner, Tag tag, String systemName,
                                            PublicMode publicMode, SolarSystemType type) {
        SolarSystem system = addSolarSystemForUser(owner, type, systemName);
        system.setPublicMode(publicMode);
        if (system.getTags() == null) {
            system.setTags(new ArrayList<>());
        }
        system.getTags().add(tag);
        system.setCurrentValues(CurrentValues.builder().build());
        return solarSystemRepository.save(system);
    }

    private SystemContributionDTO findSystem(TagAggregationDTO dto, String name) {
        return dto.getSystems().getContent().stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    @Test
    public void testAggregationUsesStoredDayValues() throws Exception {
        User owner = addUser(true, "owner");
        Tag tag = addTag("Solar", "#FFA500");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        SolarSystem system1 = createSystemWithTag(owner, tag, "System1", PublicMode.ALL, SolarSystemType.GRID);
        system1.setDayValues(DayValues.builder()
            .producedKWH(15f)
            .consumedKWH(10f)
            .gridConsumedKWH(2f)
            .gridFeedInKWH(1f)
            .localDate(today)
            .build());
        system1.setCurrentValues(CurrentValues.builder()
            .inputWatt(5000f)
            .outputWatt(2000f)
            .gridWatt(-3000f)
            .lastSet(System.currentTimeMillis())
            .build());
        solarSystemRepository.save(system1);

        SolarSystem system2 = createSystemWithTag(owner, tag, "System2", PublicMode.ALL, SolarSystemType.GRID_BATTERY);
        system2.setDayValues(DayValues.builder()
            .producedKWH(7f)
            .consumedKWH(null)
            .gridConsumedKWH(null)
            .gridFeedInKWH(null)
            .localDate(today)
            .build());
        system2.setCurrentValues(CurrentValues.builder()
            .inputWatt(1000f)
            .lastSet(System.currentTimeMillis() - 3600 * 1000)
            .build());
        solarSystemRepository.save(system2);

        String jwt = signIn("owner");

        var response = doRestRequest(
            "/api/tags/aggregation/" + tag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        // no Influx data was written, so these values can only come from stored dayValues
        Assertions.assertThat(dto.getTotalSystems()).isEqualTo(2);
        Assertions.assertThat(dto.getOnlineSystems()).isEqualTo(1);
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(22f);  // 15 + 7
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(10f);  // only system1 has consumption

        SystemContributionDTO sys1 = findSystem(dto, "System1");
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(15f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isEqualTo(10f);
        Assertions.assertThat(sys1.getCurrentProduction()).isEqualTo(5000f);
        Assertions.assertThat(sys1.getCurrentConsumption()).isEqualTo(2000f);
        Assertions.assertThat(sys1.getCurrentGrid()).isEqualTo(-3000f);
        Assertions.assertThat(sys1.isOnline()).isTrue();

        SystemContributionDTO sys2 = findSystem(dto, "System2");
        Assertions.assertThat(sys2.getDayProducedKWH()).isEqualTo(7f);
        Assertions.assertThat(sys2.getDayConsumedKWH()).isNull();
        Assertions.assertThat(sys2.isOnline()).isFalse();

        // current totals only count online systems
        Assertions.assertThat(dto.getTotalCurrentProduction()).isEqualTo(5000f);
        Assertions.assertThat(dto.getTotalCurrentConsumption()).isEqualTo(2000f);
        Assertions.assertThat(dto.getTotalCurrentGrid()).isEqualTo(-3000f);
    }

    @Test
    public void testAggregationIgnoresStaleDayValues() throws Exception {
        User owner = addUser(true, "owner");
        Tag tag = addTag("Solar", "#FFA500");

        SolarSystem system1 = createSystemWithTag(owner, tag, "System1", PublicMode.ALL, SolarSystemType.GRID);
        system1.setDayValues(DayValues.builder()
            .producedKWH(15f)
            .consumedKWH(10f)
            .localDate(LocalDate.now(ZoneOffset.UTC).minusDays(1))
            .build());
        solarSystemRepository.save(system1);

        String jwt = signIn("owner");

        var response = doRestRequest(
            "/api/tags/aggregation/" + tag.getId(),
            null,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TagAggregationDTO dto = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        // stale date -> treated as no data for today (no Influx fallback) -> 0
        Assertions.assertThat(dto.getTotalDayProducedKWH()).isEqualTo(0f);
        Assertions.assertThat(dto.getTotalDayConsumedKWH()).isEqualTo(0f);
        SystemContributionDTO sys1 = findSystem(dto, "System1");
        Assertions.assertThat(sys1.getDayProducedKWH()).isEqualTo(0f);
        Assertions.assertThat(sys1.getDayConsumedKWH()).isNull();
    }
}
