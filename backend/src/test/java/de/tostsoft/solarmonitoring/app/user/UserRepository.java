package de.tostsoft.solarmonitoring.app.user;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserRepository extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    //TODO some more tests for other repositorires
    @Test
    void checkDeleteAtWorking(){
        var user = addUser(false);
        user.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);

        var found = userRepository.findByName(user.getName());
        assertThat(found).isNotNull();

        var all = userRepository.findAll();
        assertThat(all.size()).isEqualTo(0);

        all = userRepository.seesAllFindAllByNameStartingWith("test");
        assertThat(all.size()).isEqualTo(1);
    }
}
