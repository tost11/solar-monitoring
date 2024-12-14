package de.tostsoft.solarmonitoring.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;

public class SolarSystemSearchTest  extends ApplicationBaseRestTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void addTagToSolarSystem() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        var ret = doRestRequest("api/system/search","{}", HttpMethod.GET);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
    }

}
