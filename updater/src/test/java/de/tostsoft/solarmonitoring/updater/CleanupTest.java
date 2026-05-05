package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.ConfigRepository;
import de.tostsoft.solarmonitoring.updater.service.CleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanupTest extends UpdaterBaseTest {

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private ConfigRepository configRepository;

    @BeforeEach
    public void setup() throws InterruptedException {
        clearDatabase();
    }

    @Test
    public void checkDailyRegistrationReset(){

        var config = configRepository.findAll().get(0);
        config.setDailyRegistrations(10);
        configRepository.save(config);

        cleanupService.resetDailyRegistrations();

        config = configRepository.findAll().get(0);

        assertThat(config.getDailyRegistrations()).isEqualTo(0);
    }

    @Test
    public void checkDeleteUsersPagingWorkgin(){

        for(int i=0;i<CleanupService.DELTE_USERS_PAGE_SIZE+1;i++){
            var user = addUser(false,"test"+i);
            user.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")).minusDays(356));
            userRepository.save(user);
        }

        cleanupService.runContinousCleanup();

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    public void checkusersNotFlagAsDeletedAreNotDeleted(){

        addUser(false,"test");

        cleanupService.runContinousCleanup();

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    public void checkDeleteTimeWorking(){

        var user1 = addUser(false,"test1");
        user1.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user1);

        var user2 = addUser(false,"test2");
        user2.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")).minusDays(1).minusMinutes(5));
        userRepository.save(user2);

        cleanupService.runContinousCleanup();

        var allUsers = userRepository.findAll();
        assertThat(allUsers.size()).isEqualTo(0);

        allUsers = userRepository.findAllWithDeleted();
        assertThat(allUsers.size()).isEqualTo(1);
        assertThat(allUsers.get(0).getName()).isEqualTo("test1");
    }

    @Test
    public void checkAllRelateUserDataDelete(){
        var user1 = addUser(false,"test1");
        var user2 = addUser(false,"test2");

        var system1 = addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system2 = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");

        var manages = Manages.builder()
                .solarSystem(system1)
                .user(user2)
                .permission(Permissions.ADMIN)
                .build();

        managesRepository.save(manages);

        manages = Manages.builder()
                .solarSystem(system2)
                .user(user1)
                .permission(Permissions.ADMIN)
                .build();

        managesRepository.save(manages);

        user1.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")).minusDays(356));
        userRepository.save(user1);

        cleanupService.runContinousCleanup();

        var users = userRepository.findAll();
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getName()).isEqualTo("test2");

        var system = solarSystemRepository.findAll();
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getName()).isEqualTo("test2");

        assertThat(managesRepository.count()).isEqualTo(0);
    }
}
