package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.*;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemViewDataTest  extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testViewName() throws JsonProcessingException {

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());

        var user = addUser(false);

        var jwt = signIn(user.getName());

       var registerDTO = RegisterSolarSystemDTO.builder()
               .name("Test 1")
               .type(SolarSystemType.GRID)
               .publicMode(PublicMode.PRODUCTION)
               .timezone(TimeZone.getDefault().getID())
               .namings(NamingsDTO.builder()
                       .batteries(new HashMap<>())
                       .devices(new HashMap<>())
                       .inputsAC(new HashMap<>())
                       .inputsDC(new HashMap<>())
                       .outputsAC(new HashMap<>())
                       .outputsDC(new HashMap<>())
                       .build())
               .viewData(ViewDataDTO.builder()
                       .showAmpere(true)
                       .build())
               .build();

        var res = doRestRequest("api/system",registerDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var createdSystemDTO = om.readValue(res.getBody(), RegisterSolarSystemResponseDTO.class);
        Assertions.assertThat(createdSystemDTO.getViewName()).isEqualTo("Test 1");
        Assertions.assertThat(createdSystemDTO.getName()).isEqualTo("test 1");

        var res2 = doRestRequest("api/system/"+createdSystemDTO.getId(),"", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        var systemDTO = om.readValue(res2.getBody(),SolarSystemDTO.class);
        Assertions.assertThat(systemDTO.getViewName()).isEqualTo("Test 1");
        Assertions.assertThat(systemDTO.getName()).isEqualTo("test 1");

        var res3 = doRestRequest("api/system/search", "{}", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
        var list = objectMapper.readValue(res3.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getName()).isEqualTo("Test 1");

        var tag = tagRepository.save(Tag.builder().name("test2").color("#fffffff").viewName("test2").locked(false).showOnStartPage(true).build());
        var system = solarSystemRepository.findById(createdSystemDTO.getId()).get();
        system.setTags(Collections.singletonList(tag));
        solarSystemRepository.save(system);

        var ret = doRestRequest("api/tags/systems");
        var list2 = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {});

        Assertions.assertThat(list2).hasSize(1);
        Assertions.assertThat(list2.get(0).getSystems()).hasSize(1);
        Assertions.assertThat(list2.get(0).getSystems().get(0).getName()).isEqualTo("Test 1");
    }
}
