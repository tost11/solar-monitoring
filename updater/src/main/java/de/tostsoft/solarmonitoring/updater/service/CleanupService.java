package de.tostsoft.solarmonitoring.updater.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private CaptchaRepository captchaRepository;

    @Scheduled(cron = "0 1 * * * *")
    public void runDailyCleanup() {
        LOG.info("----- started daily cleanup script -----");

        try{
            checkUnfinishedUsers();
        }catch (Exception e){
            LOG.error("Error checking for data of unfinished users",e);
        }

        LOG.info("----- ended daily cleanup script -----");
    }

    @Scheduled(fixedDelayString = "${timing.continuousCleanup.delay:300000}", initialDelayString = "${timing.continuousCleanup.start:0}")
    public void runContinousCleanup() {
        LOG.info("----- started continous cleanup script -----");

        try{
            deleteOldCaptchas();
        }catch (Exception e){
            LOG.error("Error checking for old captchas",e);
        }

        LOG.info("----- ended continous cleanup script -----");
    }

    private boolean isPreservedName(String string) {
        if(StringUtils.startsWith(string,"_")){
            return true;
        }
        return StringUtils.equals(string,"my-bucket");
    }

    private void checkUnfinishedUsers(){
        LOG.info("-> check unfinished users");

        ArrayList<String> toDeleteBucket = new ArrayList<>();

        List<Bucket> buckets = influxConnection.getBuckets();
        for (Bucket bucket : buckets) {

            if (!isPreservedName(bucket.getName())) {
                LOG.debug("Bucket is preserved: {}",bucket.getName());
                continue;
            }
            var userOpt = userRepository.findByInfluxBucketName(bucket.getName());
            if (userOpt.isEmpty() && bucket.getCreatedAt().isAfter(OffsetDateTime.now().minus(5,ChronoUnit.MINUTES))) {
                toDeleteBucket.add(bucket.getName());
            }
        }
        LOG.info("Found {} Influx buckets to be deleted", toDeleteBucket.size());
        for (String bucketName : toDeleteBucket) {
            if (bucketName != null) {
                LOG.info("Delete Influx Bucket (not really done) " + bucketName);
                //influxConnection.deleteBucket(bucketName);
            }
        }
        LOG.info("Deleted {} Influx buckets", toDeleteBucket.size());
    }

    protected void deleteOldCaptchas(){
        LOG.info("-> check old captchas");

        long all = captchaRepository.count();
        Instant now = Instant.now();

        //delete all captchas older than one hour
        var stamp = now.minus(1, ChronoUnit.HOURS);
        captchaRepository.deleteAllByCreatedAtBefore(stamp.toEpochMilli());

        long dif = all - captchaRepository.count();
        dif = Math.max(0, dif);

        LOG.info("Cleaned up "+dif+" captchas");
    }

    //TODO register user cleanup
}

