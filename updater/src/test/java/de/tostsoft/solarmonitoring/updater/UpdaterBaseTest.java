package de.tostsoft.solarmonitoring.updater;

import com.influxdb.client.BucketsQuery;
import com.influxdb.client.FindOptions;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Run;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private JWTSessionTokenRepository jwtSessionTokenRepository;

    @Value("${configNode:root}")
    private String configName;

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
                //delete again
                influxConnection.deleteBucket(bucket.getName());
            }

            if(count == 0){
                return;
            }

            Thread.sleep(500);
        }

        throw new RuntimeException("influx buckets not deleted in time");
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
        configRepository.deleteAll();
        jwtSessionTokenRepository.deleteAll();

        mailhogTestService.deleteAllMessages();

        //init
        configRepository.save(Config.builder()
                .dailyRegistrations(0)
                .isRegistrationEnabled(true)
                .name(configName)
                .build());
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

        for(int i=0;i<=10;i++){

            if(i == 10){
                throw new RuntimeException("could not create bucket");
            }

            var bq = new BucketsQuery();
            bq.setName(user.getInfluxBucketName());
            var buckets = influxConnection.getClient().getBucketsApi().findBuckets(bq);

            if(!buckets.isEmpty()){
                break;
            }

            try {
                Thread.sleep(500);
            }catch (InterruptedException e){}
        }

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
