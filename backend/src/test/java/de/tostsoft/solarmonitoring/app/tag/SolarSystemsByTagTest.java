package de.tostsoft.solarmonitoring.app.tag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.service.TagService;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class SolarSystemsByTagTest extends AppBaseTest {

    @Autowired
    private TagService tagService;

    @BeforeEach
    public void prepare() throws NoSuchFieldException, IllegalAccessException {
        clearDatabase();

        Field field1 = tagService.getClass().getDeclaredField("cachedPublicSystemsByTagTime");
        field1.setAccessible(true);
        field1.setInt(tagService, 0);//no caching
    }

    @Test
    public void checkTagsShownBecauseNoSystemsByThem() throws JsonProcessingException {

        tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>(){});

        Assertions.assertThat(list).hasSize(0);
    }

    @Test
    public void checkTagShown() throws JsonProcessingException {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setShortener("short");
        system.setPublicMode(PublicMode.ALL);

        solarSystemRepository.save(system);

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems()).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems().get(0).getId()).isEqualTo(system.getId());

        var systemDTO = list.get(0).getSystems().get(0);
        Assertions.assertThat(systemDTO.getId()).isEqualTo(system.getId());
        Assertions.assertThat(systemDTO.getName()).isEqualTo(system.getSystemInformations().getViewName());
        Assertions.assertThat(systemDTO.getType()).isEqualTo(system.getType());
        Assertions.assertThat(systemDTO.getRole()).isEqualTo("public");
        Assertions.assertThat(systemDTO.getShortener()).isEqualTo(system.getShortener());

        var tagDTO = list.get(0).getTag();
        Assertions.assertThat(tagDTO.getId()).isEqualTo(tag.getId());
        Assertions.assertThat(tagDTO.getColor()).isEqualTo(tag.getColor());
        Assertions.assertThat(tagDTO.getName()).isEqualTo(tag.getViewName());
    }

    @Test
    public void checkMultipleTagsAndSystems() throws JsonProcessingException {
        var user = addUser(false);
        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        system1.setPublicMode(PublicMode.ALL);
        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");
        system2.setPublicMode(PublicMode.ALL);

        var tag1 = tagRepository.save(Tag.builder().viewName("Test1").name("test1").locked(true).color("#ffffff").showOnStartPage(true).build());
        var tag2 = tagRepository.save(Tag.builder().viewName("Test2").name("test2").locked(true).color("#ffffff").showOnStartPage(true).build());

        system1.setTags(Collections.singletonList(tag1));
        system2.setTags(Arrays.asList(tag1, tag2));

        solarSystemRepository.save(system1);
        solarSystemRepository.save(system2);

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {
        });

        Assertions.assertThat(list).hasSize(2);
        Assertions.assertThat(list.stream().filter(ts->ts.getTag().getId().equals(tag1.getId())).findAny().get().getSystems()).hasSize(2);
        Assertions.assertThat(list.stream().filter(ts->ts.getTag().getId().equals(tag2.getId())).findAny().get().getSystems()).hasSize(1);
    }

    private static Stream<Arguments> checkSystemPublicModeShownOrNotArguments() {
        return Stream.of(
                Arguments.of(PublicMode.NONE, 0),
                Arguments.of(PublicMode.ALL, 1),
                Arguments.of(PublicMode.PRODUCTION, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("checkSystemPublicModeShownOrNotArguments")
    public void checkSystemPublicModeShownOrNot(PublicMode mode,int size) throws JsonProcessingException {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(mode);

        solarSystemRepository.save(system);

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {});

        Assertions.assertThat(list).hasSize(size);
    }

    @Test
    public void checkSystemOwnsShown() throws JsonProcessingException {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.NONE);

        solarSystemRepository.save(system);

        var jwt = signIn();

        var ret = doRestRequest("api/tags/systems","",HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems().get(0).getRole()).isEqualTo("owns");
    }

    private static Stream<Arguments> checkSystemOwnsShownArguments() {
        return Stream.of(
                Arguments.of(Permissions.ADMIN, "owns"),
                Arguments.of(Permissions.MANAGE, "manages"),
                Arguments.of(Permissions.VIEW, "view")
        );
    }

    @ParameterizedTest
    @MethodSource("checkSystemOwnsShownArguments")
    public void checkSystemOwnsShown(Permissions permissions,String role) throws JsonProcessingException {
        var user = addUser(false,"test1");
        var user2 = addUser(false,"test2");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(system);

        var manages = Manages.builder()
                .solarSystem(system)
                .user(user2)
                .permission(permissions)
                .build();
        managesRepository.save(manages);

        var jwt = signIn("test2");

        var ret = doRestRequest("api/tags/systems","",HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems().get(0).getRole()).isEqualTo(role);
    }

    private static Stream<Arguments> checkCurrentValuesCorrectPublicArguments() {
        return Stream.of(
                Arguments.of(PublicMode.ALL, true,true),
                Arguments.of(PublicMode.PRODUCTION, false,true)
        );
    }

    @ParameterizedTest
    @MethodSource("checkCurrentValuesCorrectPublicArguments")
    public void checkCurrentValuesCorrectPublic(PublicMode publicMode,boolean consumptionShown,boolean productionShown) throws JsonProcessingException, InterruptedException {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(publicMode);

        solarSystemRepository.save(system);

        var sample = new SampleDTO();
        sample.setDuration(30.f);
        sample.setInputWattDC(100.f);
        sample.setBatteryVoltage(12.f);
        sample.setBatteryPercentage(75.f);
        sample.setBatteryWatt(20.f);
        sample.setOutputWatt(50.f);
        sample.setGridWatt(10.f);

        pushDataSample(system.getId(),sample);

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>(){});

        if(consumptionShown){
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryVoltage()).isEqualTo(12.f);
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryPercentage()).isEqualTo(75.f);
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryWatt()).isEqualTo(20.f);
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getOutputWatt()).isEqualTo(50.f);
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getGridWatt()).isEqualTo(10.f);
        }
        if(productionShown){
            Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getInputWatt()).isEqualTo(100.f);
        }
    }

    @Test
    public void checkCurrentValuesOnOwnsSystem() throws JsonProcessingException, InterruptedException {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.NONE);

        solarSystemRepository.save(system);

        var sample = new SampleDTO();
        sample.setDuration(30.f);
        sample.setInputWattDC(100.f);
        sample.setBatteryVoltage(12.f);
        sample.setBatteryPercentage(75.f);
        sample.setBatteryWatt(20.f);
        sample.setOutputWatt(50.f);
        sample.setGridWatt(10.f);

        pushDataSample(system.getId(),sample);

        var jwt = signIn();

        var ret = doRestRequest("api/tags/systems","",HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>(){});

        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getInputWatt()).isEqualTo(100.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getOutputWatt()).isEqualTo(50.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getGridWatt()).isEqualTo(10.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryVoltage()).isEqualTo(12.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryPercentage()).isEqualTo(75.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryWatt()).isEqualTo(20.f);
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void checkCurrentValuesManagesRelations(Permissions permissions) throws JsonProcessingException {
        var user = addUser(false,"test1");
        var user2 = addUser(false,"test2");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(system);

        var manages = Manages.builder()
                .solarSystem(system)
                .user(user2)
                .permission(permissions)
                .build();
        managesRepository.save(manages);

        var sample = new SampleDTO();
        sample.setDuration(30.f);
        sample.setInputWattDC(100.f);
        sample.setBatteryVoltage(12.f);
        sample.setBatteryPercentage(75.f);
        sample.setBatteryWatt(20.f);
        sample.setOutputWatt(50.f);
        sample.setGridWatt(10.f);

        pushDataSample(system.getId(),sample);

        var jwt = signIn("test2");

        var ret = doRestRequest("api/tags/systems","",HttpMethod.GET,Collections.singletonMap("Cookie","jwt="+jwt));
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {});

        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getInputWatt()).isEqualTo(100.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getOutputWatt()).isEqualTo(50.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getGridWatt()).isEqualTo(10.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryVoltage()).isEqualTo(12.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryPercentage()).isEqualTo(75.f);
        Assertions.assertThat(list.get(0).getSystems().get(0).getCurrentValues().getBatteryWatt()).isEqualTo(20.f);
    }

    @Test
    public void checkPublicCachingWorking() throws JsonProcessingException, InterruptedException, NoSuchFieldException, IllegalAccessException {

        Field field1 = tagService.getClass().getDeclaredField("cachedPublicSystemsByTagTime");
        field1.setAccessible(true);
        field1.setInt(tagService, 0);

        var user = addUser(false);
        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system1.setTags(Collections.singletonList(tag));
        system1.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system1);

        var ret = doRestRequest("api/tags/systems");
        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {
        });

        
        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems()).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems().get(0).getId()).isEqualTo(system1.getId());

        system2.setTags(Collections.singletonList(tag));
        system2.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system2);

        int testDuration = 3;
        field1 = tagService.getClass().getDeclaredField("cachedPublicSystemsByTagTime");
        field1.setAccessible(true);
        field1.setInt(tagService, testDuration);

        ret = doRestRequest("api/tags/systems");
        list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {
        });

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems()).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems().get(0).getId()).isEqualTo(system1.getId());

        Thread.sleep(Duration.ofSeconds(testDuration).toMillis());

        ret = doRestRequest("api/tags/systems");
        list = objectMapper.readValue(ret.getBody(), new TypeReference<List<TagSolarSystemDTO>>() {
        });

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getSystems()).hasSize(2);
    }
}
