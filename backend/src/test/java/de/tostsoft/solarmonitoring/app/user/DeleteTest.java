package de.tostsoft.solarmonitoring.app.user;


import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.JwtUtil;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DeleteTest extends AppBaseTest {

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    public void prepare() throws NoSuchFieldException, IllegalAccessException {
        clearDatabase();
    }

    @Test
    void UserDeleteTest(){
        addUser(false);

        var jwt = signIn();

        var res = doRequest("/api/user", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void UserDeleteInvalidateCookieTest(){
        var user = addUser(false,"test");

        var jwt = signIn();

        var res = doRequest("/api/user", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        var ex = assertThrows(RuntimeException.class,()->jwtUtil.validateToken(jwt,user));
        assertThat(ex.getMessage()).isEqualTo("Token isn't valid any more");
    }

    @Test
    void UserDeleteCheckAllRelationDeleted(){
        var user1 = addUser(false,"test1");
        var user2 = addUser(false,"test2");

        var system1 = addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system2 = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");

        var manges1 = Manages.builder()
                .permission(Permissions.ADMIN)
                .solarSystem(system1)
                .user(user2)
                .build();

        var manges2 = Manages.builder()
                .permission(Permissions.ADMIN)
                .solarSystem(system2)
                .user(user1)
                .build();

        managesRepository.save(manges1);
        managesRepository.save(manges2);

        var jwt = signIn("test1");

        var res = doRequest("/api/user", HttpMethod.DELETE, Collections.singletonMap("Cookie","jwt="+jwt));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(solarSystemRepository.findAll()).hasSize(1);
        assertThat(managesRepository.findAll()).hasSize(0);
    }

}
