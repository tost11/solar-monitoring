package de.tostsoft.solarmonitoring.app.manages;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class MangesTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void checkDeleteAttWorking(){

        var user1 = addUser(false,"test1");
        var user2 = addUser(false,"test2");

        var system = addSolarSystemForUser(user1, SolarSystemType.GRID);

        var manges = Manages.builder()
                .id(null)
                .user(user2)
                .permission(Permissions.ADMIN)
                .solarSystem(system)
                .build();

        manges = managesRepository.save(manges);

        assertThat(managesRepository.findById(manges.getId())).isNotEmpty();

        manges.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        manges = managesRepository.save(manges);

        assertThat(managesRepository.findById(manges.getId())).isEmpty();
    }
}
