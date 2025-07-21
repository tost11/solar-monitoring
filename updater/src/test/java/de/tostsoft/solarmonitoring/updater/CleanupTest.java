package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.ConfigRepository;
import de.tostsoft.solarmonitoring.lib.repository.JWTSessionTokenRepository;
import de.tostsoft.solarmonitoring.lib.repository.RegisterUserRepository;
import de.tostsoft.solarmonitoring.updater.service.CleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanupTest extends UpdaterBaseTest {

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private CaptchaRepository captchaRepository;

    @Autowired
    private RegisterUserRepository registerUserRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private JWTSessionTokenRepository jWTSessionTokenRepository;

    @BeforeEach
    public void setup() throws InterruptedException {
        clearDatabase();
    }

    @Test
    public void checkValidCaptchasNotCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE1");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        captchaRepository.save(capt);

        capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE2");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        capt.setCreatedAt(Instant.now().minus(55, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE3");
        capt.setText("TEST_TEXT");
        capt.setCreatedAt(Instant.now().toEpochMilli());
        capt.setCreatedAt(Instant.now().minus(65, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(2);
    }

    @Test
    public void checkOldCaptchasCleanedUp(){

        var capt = new Captcha();
        capt.setBase64Image("TEST_IMAGE");
        capt.setText("TEST_TEXT");

        capt.setCreatedAt(Instant.now().minus(65, ChronoUnit.MINUTES).toEpochMilli());
        captchaRepository.save(capt);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(captchaRepository.count()).isEqualTo(0);
        }

    @Test
    public void checkValidRegisterUsersNotCleanedUp(){

        var user = RegisterUser.builder()
                        .mail("test@local.host")
                        .password("abcTest123!")
                        .name("test")
                        .viewName("Test")
                        .createdAt(Instant.now().toEpochMilli())
                        .build();

        registerUserRepository.save(user);

        user = RegisterUser.builder()
                .mail("test2@local.host")
                .password("abcTest123!")
                .name("test2")
                .viewName("Test2")
                .createdAt(Instant.now().toEpochMilli())
                .build();

        user.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(55,ChronoUnit.MINUTES).toEpochMilli());
        registerUserRepository.save(user);


        user = RegisterUser.builder()
                .mail("test3@local.host")
                .password("abcTest123!")
                .name("test3")
                .viewName("Test3")
                .createdAt(Instant.now().toEpochMilli())
                .build();

        user.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(65,ChronoUnit.MINUTES).toEpochMilli());
        registerUserRepository.save(user);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(registerUserRepository.count()).isEqualTo(2);
    }

    @Test
    public void checkOldRegisterUsersCleanedUp(){

        var user = RegisterUser.builder()
                .mail("test@local.host")
                .password("abcTest123!")
                .name("test")
                .viewName("Test")
                .createdAt(Instant.now().minus(24, ChronoUnit.HOURS).minus(5,ChronoUnit.MINUTES).toEpochMilli())
                .build();

        registerUserRepository.save(user);

        //defautl delete time is one hour
        cleanupService.runContinousCleanup();

        assertThat(registerUserRepository.count()).isEqualTo(0);
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

    @Test
    public void checkCleanupJWTSessionTokens(){

        var user = addUser(false);

        jWTSessionTokenRepository.save(JWTSessionToken.builder()
                .ownedBy(user)
                .validUntil(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(5))
                .build());

        var token2 = jWTSessionTokenRepository.save(JWTSessionToken.builder()
                .ownedBy(user)
                .validUntil(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(5))
                .build());

        cleanupService.cleanUpOldSessionTokens();

        var allUsers = jWTSessionTokenRepository.findAll();
        assertThat(allUsers.size()).isEqualTo(1);
        assertThat(allUsers.get(0).getId()).isEqualTo(token2.getId());
    }

}
