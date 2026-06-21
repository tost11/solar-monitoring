package de.tostsoft.solarmonitoring.app.solarsystem;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SolarSystemDeleteTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testDeleteSolarSystem() throws Exception {
        addUser(true);
        var jwt = signIn();

        var systemDTO = SolarSystemControllerTest.crateDefaultRegisterDTO();

        ResponseEntity<String> createResponse = doRestRequest(
            "api/system",
            systemDTO,
            HttpMethod.POST,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var system = solarSystemRepository.findAll().get(0);
        String systemId = system.getId();

        ResponseEntity<String> getResponse = doRequest(
            "api/system/" + systemId,
            HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> deleteResponse = doRequest(
            "api/system/" + systemId,
            HttpMethod.DELETE,
            Collections.singletonMap("Cookie", "jwt=" + jwt)
        );

        Assertions.assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(deleteResponse.getBody()).isEqualTo("System is Deleted");

        var deletedSystem = solarSystemRepository.findByIdWithDeleted(systemId);

        Assertions.assertThat(deletedSystem).isPresent();
        Assertions.assertThat(deletedSystem.get().getDeletedAt()).isNotNull();
        Assertions.assertThat(deletedSystem.get().getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        var exception = org.junit.jupiter.api.Assertions.assertThrows(HttpClientErrorException.class, () -> {
            doRequest(
                "api/system/" + systemId,
                HttpMethod.GET,
                Collections.singletonMap("Cookie", "jwt=" + jwt)
            );
        });
        Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var standardFind = solarSystemRepository.findAllByIdOrShortener(systemId);
        Assertions.assertThat(standardFind).isEmpty();

        var allSystems = solarSystemRepository.findAll();
        Assertions.assertThat(allSystems).noneMatch(s -> s.getId().equals(systemId));
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void testDeleteSystemWithManagedPermission(Permissions permission) throws Exception {
        User owner = addUser(false, "owner");
        var ownerJwt = signIn("owner");

        var systemDTO = SolarSystemControllerTest.crateDefaultRegisterDTO();
        doRestRequest("api/system", systemDTO, HttpMethod.POST,
            Collections.singletonMap("Cookie", "jwt=" + ownerJwt));

        var system = solarSystemRepository.findAll().get(0);
        String systemId = system.getId();

        User managedUser = addUser(false, "manageduser");
        Manages manages = Manages.builder()
            .solarSystem(system)
            .user(managedUser)
            .permission(permission)
            .build();
        managesRepository.save(manages);

        var managedJwt = signIn("manageduser");

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class, () -> {
                doRequest("api/system/" + systemId, HttpMethod.DELETE,
                    Collections.singletonMap("Cookie", "jwt=" + managedJwt));
            }
        );

        Assertions.assertThat(exception.getStatusCode())
            .isIn(HttpStatus.FORBIDDEN);

        var systemAfter = solarSystemRepository.findByIdWithDeleted(systemId);
        Assertions.assertThat(systemAfter).isPresent();
        Assertions.assertThat(systemAfter.get().getDeletedAt()).isNull();
    }

    @Test
    public void testDeleteSystemWithoutPermission() throws Exception {
        User owner = addUser(false, "owner");
        var ownerJwt = signIn("owner");

        var systemDTO = SolarSystemControllerTest.crateDefaultRegisterDTO();
        doRestRequest("api/system", systemDTO, HttpMethod.POST,
            Collections.singletonMap("Cookie", "jwt=" + ownerJwt));

        var system = solarSystemRepository.findAll().get(0);
        String systemId = system.getId();

        User otherUser = addUser(false, "otheruser");
        var otherJwt = signIn("otheruser");

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class, () -> {
                doRequest("api/system/" + systemId, HttpMethod.DELETE,
                    Collections.singletonMap("Cookie", "jwt=" + otherJwt));
            }
        );

        Assertions.assertThat(exception.getStatusCode())
            .isIn(HttpStatus.FORBIDDEN);

        var systemAfter = solarSystemRepository.findByIdWithDeleted(systemId);
        Assertions.assertThat(systemAfter).isPresent();
        Assertions.assertThat(systemAfter.get().getDeletedAt()).isNull();
    }

    @Test
    public void testDeleteSystemUnauthenticated() throws Exception {
        User owner = addUser(false, "owner");
        var ownerJwt = signIn("owner");

        var systemDTO = SolarSystemControllerTest.crateDefaultRegisterDTO();
        doRestRequest("api/system", systemDTO, HttpMethod.POST,
            Collections.singletonMap("Cookie", "jwt=" + ownerJwt));

        var system = solarSystemRepository.findAll().get(0);
        String systemId = system.getId();

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class, () -> {
                doRequest("api/system/" + systemId, HttpMethod.DELETE,
                    Collections.emptyMap());
            }
        );

        Assertions.assertThat(exception.getStatusCode())
            .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

        var systemAfter = solarSystemRepository.findByIdWithDeleted(systemId);
        Assertions.assertThat(systemAfter).isPresent();
        Assertions.assertThat(systemAfter.get().getDeletedAt()).isNull();
    }
}
