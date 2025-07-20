package de.tostsoft.solarmonitoring.app.solarsystem;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class SolarSystemTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void checkUniqueShortnerRestrainAnZero(){

        var user = addUser(false);

        var system1 = SolarSystem.builder()
                .id(null)
                .name("test1")
                .viewName("Test1")
                .timezone("UTC")
                .creationDate(LocalDateTime.now(ZoneOffset.UTC))
                .viewData(new ViewData())
                .influxTagName("Test1")
                .shortener(null)
                .ownedBy(user)
                .totalValues(new TotalValues())
                .build();

        var system2 = SolarSystem.builder()
                .id(null)
                .name("test2")
                .viewName("Test2")
                .timezone("UTC")
                .influxTagName("Test2")
                .creationDate(LocalDateTime.now(ZoneOffset.UTC))
                .viewData(new ViewData())
                .shortener(null)
                .ownedBy(user)
                .totalValues(new TotalValues())
                .build();

        solarSystemRepository.save(system1);
        solarSystemRepository.save(system2);
    }

    @Test
    public void checkDeleteAttWorking(){

        var user = addUser(false);

        var system = SolarSystem.builder()
                .id(null)
                .ownedBy(user)
                .name("test1")
                .viewName("Test1")
                .timezone("UTC")
                .creationDate(LocalDateTime.now(ZoneOffset.UTC))
                .viewData(new ViewData())
                .influxTagName("Test1")
                .totalValues(TotalValues.builder().build())
                .shortener(null)
                .build();

        system = solarSystemRepository.save(system);

        assertThat(solarSystemRepository.findById(system.getId())).isNotEmpty();

        system.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        system = solarSystemRepository.save(system);

        assertThat(solarSystemRepository.findById(system.getId())).isEmpty();
    }

}
