package de.tostsoft.solarmonitoring.solarsystem;

import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemTagTest extends ApplicationBaseRestTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void addTagToSolarSystem() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"POST", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(1);
        Assertions.assertThat(system.getTags().get(0).getId()).isEqualTo(tag.getId());
    }

    @Test
    public void addTagToSolarSystemTwice() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"POST", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"POST", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(1);
        Assertions.assertThat(system.getTags().get(0).getId()).isEqualTo(tag.getId());
    }

    @Test
    public void removeTagFromSolarSystem() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        system.setTags(Collections.singletonList(tag));

        system = solarSystemRepository.save(system);

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(0);
    }

    @Test
    public void removeTagFromSolarSystemTwice() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        system.setTags(Collections.singletonList(tag));

        system = solarSystemRepository.save(system);

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));
        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(0);
    }

    @Test
    public void addTagToNoAccess() {
        var user = addUser(true);
        addUser(true,"test2");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn("test2");

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void removeTagToNoAccess() {
        var user = addUser(true);
        addUser(true,"test2");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn("test2");

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void addTagMissingParameterSystemId() {
        addUser(true);
        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId=WHATEVER","", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void removeTagMissingParameterSystemId() {
        addUser(true);
        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId=WHATEVER","", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void addTagMissingParameterTagId() {
        addUser(true);
        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?systemId=WHATEVER","", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void removeTagMissingParameterTagId() {
        addUser(true);
        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?systemId=WHATEVER","", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void addTagSystemNotFound() {
        addUser(true);
        var jwt = signIn();

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId="+tag.getId()+"&systemId=WHATEVER","", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void removeTagSystemNotFound() {
        addUser(true);
        var jwt = signIn();

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(false).color("#ffffff").showOnStartPage(false).build());

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId="+tag.getId()+"&systemId=WHATEVER","", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void addTagTagNotFound() {
        var user = addUser(true);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId=WHATEVER&systemId="+system.getId(),"", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void removeTagTagNotFound() {
        var user = addUser(true);
        var jwt = signIn();

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId=WHATEVER&systemId="+system.getId(),"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void addLockedTagToSolarSystem() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"POST", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(1);
        Assertions.assertThat(system.getTags().get(0).getId()).isEqualTo(tag.getId());
    }

    @Test
    public void removeLockedTagToSolarSystem() {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(false).build());

        system.setTags(Collections.singletonList(tag));
        system = solarSystemRepository.save(system);

        var jwt = signIn();

        doRestRequest("api/system/tag?systemId="+system.getId()+"&tagId="+tag.getId(),"POST", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        system = solarSystemRepository.findById(system.getId()).get();

        Assertions.assertThat(system.getTags()).hasSize(0);
    }

    @Test
    public void addLockedTagNoAllowedToSolarSystem() {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(false).build());

        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId="+tag.getId()+"&systemId="+system.getId(),"", HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void removeLockedTagNotAllowedToSolarSystem() {
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        var tag = tagRepository.save(Tag.builder().viewName("Test").name("test").locked(true).color("#ffffff").showOnStartPage(false).build());

        system.setTags(Collections.singletonList(tag));
        system = solarSystemRepository.save(system);

        var systemId = system.getId();

        var jwt = signIn();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/tag?tagId="+tag.getId()+"&systemId="+systemId,"", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
