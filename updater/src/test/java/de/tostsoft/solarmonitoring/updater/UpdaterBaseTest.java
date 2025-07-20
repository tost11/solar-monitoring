package de.tostsoft.solarmonitoring.updater;

import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Run;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.TotalValues;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.ViewData;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@SpringBootTest(classes = {SolarmonitoringUpdater.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdaterBaseTest {

    @Autowired
    protected InfluxConnection influxConnection;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RegisterUserRepository registerUserRepository;

    @Autowired
    protected CaptchaRepository captchaRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected SolarSystemRepository solarSystemRepository;

    @Autowired
    protected ManagesRepository managesRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected MailhogTestService mailhogTestService;

    private void deleteAllBuckets() throws InterruptedException {
        for (Bucket bucket : influxConnection.getBuckets()) {
            if(bucket.getName().startsWith("_")){//skip system buckets
                continue;
            }
            influxConnection.deleteBucket(bucket.getName());
        }

        //wait for influx to delete buckets
        for(int i=0;i<10;i++){
            int count = 0;
            for (Bucket bucket : influxConnection.getBuckets()) {
                if(bucket.getName().startsWith("_")){//skip system buckets
                    continue;
                }
                count++;
            }

            if(count == 0){
                return;
            }

            Thread.sleep(500);
        }

        throw new RuntimeException("influx bueckets not deleted in time");
    }

    protected void clearDatabase() throws InterruptedException {

        deleteAllBuckets();

        userRepository.deleteAll();
        solarSystemRepository.deleteAll();
        managesRepository.deleteAll();
        tagRepository.deleteAll();
        notificationRepository.deleteAll();
        registerUserRepository.deleteAll();
        captchaRepository.deleteAll();

        mailhogTestService.deleteAllMessages();
    }

    protected User addUser(boolean admin){
        return addUser(admin,"test");
    }

    protected User addUser(boolean admin,String name){
        var user = User.builder()
                .name(StringUtils.toRootLowerCase(name))
                .viewName(name)
                .password("NOT_A_VALID_PASSWORD")
                .isAdmin(admin)
                .mail(StringUtils.toRootLowerCase(name)+"@local.host")
                .creationDate(LocalDateTime.now(ZoneOffset.UTC))
                .numAllowedSystems(100)
                .viewName(name.toUpperCase())
                .influxBucketName(name)
                .build();

        influxConnection.createNewBucket(user.getInfluxBucketName());

        return userRepository.save(user);
    }


    protected SolarSystem addSolarSystemForUser(User user, SolarSystemType type){
        return addSolarSystemForUser(user,type,"test");
    }

    protected SolarSystem addSolarSystemForUser(User user,SolarSystemType type,String name){
        var system = SolarSystem.builder()
                .name(name)
                .viewName(name.toUpperCase())
                .type(type)
                .creationDate(LocalDateTime.now())
                .influxTagName(name)
                .token("NOT_A_VALID_TOKEN")
                .ownedBy(user)
                .publicMode(PublicMode.NONE)
                .timezone("UTC")
                .totalValues(TotalValues.builder().build())
                .viewData(ViewData.builder().build())
                .build();

        return solarSystemRepository.save(system);
    }


}
