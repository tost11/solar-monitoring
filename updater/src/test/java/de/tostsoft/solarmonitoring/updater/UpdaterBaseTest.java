package de.tostsoft.solarmonitoring.updater;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    protected void clearDatabase() {

        for (Bucket bucket : influxConnection.getBuckets()) {
            if(bucket.getName().startsWith("_")){//skip system buckets
                continue;
            }
            influxConnection.deleteBucket(bucket.getName());
        }
        userRepository.deleteAll();
        solarSystemRepository.deleteAll();
        managesRepository.deleteAll();
        tagRepository.deleteAll();
        notificationRepository.deleteAll();
        registerUserRepository.deleteAll();
        captchaRepository.deleteAll();

        mailhogTestService.deleteAllMessages();
    }

}
