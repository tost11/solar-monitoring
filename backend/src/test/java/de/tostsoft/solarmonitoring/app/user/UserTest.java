package de.tostsoft.solarmonitoring.app.user;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void checkUniqueMailRestrainAnZero(){

        var user1 = addUser(false,"test1");

        var user2 = addUser(false,"test2");

        userRepository.save(user1);
        userRepository.save(user2);
    }

    @Test
    public void checkDeleteAttWorking(){
        var user = addUser(false);

        user = userRepository.save(user);

        assertThat(userRepository.findById(user.getId())).isNotEmpty();

        user.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        user = userRepository.save(user);

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

}
