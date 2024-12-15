package de.tostsoft.solarmonitoring.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemSearchDTO;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemSearchTest  extends ApplicationBaseRestTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void emtpySearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
    }


    @Test
    public void PublicTrueSearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @Test
    public void PublicFalseSearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        //this is still 1 one because user is not signed in so something have to be returned
        Assertions.assertThat(list).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(SolarSystemType.class)
    public void TypeSearchTest(SolarSystemType type) throws JsonProcessingException {
        var user = addUser(true);

        SolarSystem system = null;

        for(var typeToAdd : SolarSystemType.values()){
            var sys = addSolarSystemForUser(user, typeToAdd,"system-"+typeToAdd);
            sys.setPublicMode(PublicMode.ALL);
            sys = solarSystemRepository.save(sys);
            if(typeToAdd == type){
                system = sys;
            }
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setType(type);

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getType()).isEqualTo(type);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"est","test","this-test-system","this","system","TEST","THIS-TEST-System","s-t"})
    public void checkNameTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"NOT_CONTAINING_SEARCH_TERM");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        system = solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {".*test.*","\\Qtest\\E"})//check if regex is escaped
    public void checkNameNotFoundTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"","e","es"})//check if regex is escaped
    public void checkNameNotValidTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/search",searchDTO, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkTagsTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Collections.singletonList(tag.getId()));

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }


    @Test
    public void checkTwoTagsTest() throws JsonProcessingException {
        var user = addUser(true);

        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var tag1 = tagRepository.save(Tag.builder().viewName("Test1").name("test1").locked(true).color("#ffffff").showOnStartPage(true).build());
        system.setTags(Collections.singletonList(tag1));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        system = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");
        var tag2 = tagRepository.save(Tag.builder().viewName("Test2").name("test2").locked(true).color("#ffffff").showOnStartPage(true).build());
        system.setTags(Collections.singletonList(tag1));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Arrays.asList(tag1.getId(), tag2.getId()));

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(2);
    }

    @Test
    public void checkTwoTagsSameSystemReturnDestictTest() throws JsonProcessingException {
        var user = addUser(true);

        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var tag1 = tagRepository.save(Tag.builder().viewName("Test1").name("test1").locked(true).color("#ffffff").showOnStartPage(true).build());
        var tag2 = tagRepository.save(Tag.builder().viewName("Test2").name("test2").locked(true).color("#ffffff").showOnStartPage(true).build());

        system.setTags(Arrays.asList(tag1,tag2));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Arrays.asList(tag1.getId(), tag2.getId()));

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST);

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
    }


    @Test
    public void permissionOwnsTest() throws JsonProcessingException {
        var user1 = addUser(true,"user1");
        var user2 = addUser(true,"user2");

        addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @Test
    public void permissionOwnsTestAndPublic() throws JsonProcessingException {
        var user1 = addUser(true,"user1");
        var user2 = addUser(true,"user2");

        addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var jwt = signIn("user1");

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void permissonManagesTest(Permissions permission) throws JsonProcessingException {
        addUser(true,"user1");
        var user2 = addUser(true,"user2");

        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test");
        var manages = Manages.builder().solarSystem(system).user(user2).permission(permission).build();
        managesRepository.save(manages);

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void permissonManagesAndPublicDesctinctTest(Permissions permission) throws JsonProcessingException {
        addUser(true,"user1");
        var user2 = addUser(true,"user2");

        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test");
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);
        var manages = Manages.builder().solarSystem(system).user(user2).permission(permission).build();
        managesRepository.save(manages);

        var solarSystemSearchDTO = new SolarSystemSearchDTO();
        solarSystemSearchDTO.setIsPublic(true);

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search",solarSystemSearchDTO, HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var list = objectMapper.readValue(ret.getBody(), new TypeReference<List<SolarSystemListItemDTO>>(){});

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

}
