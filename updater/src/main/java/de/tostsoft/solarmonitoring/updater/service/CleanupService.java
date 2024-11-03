package de.tostsoft.solarmonitoring.updater.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @PostConstruct
    public void runInitialCleanup() {
        cleanup();
    }

    @Scheduled(cron = "0 1 * * * *")
    public void runDailyCleanup() {
        cleanup();
    }

    private boolean isPreservedName(String string) {
        if(StringUtils.startsWith(string,"_")){
            return true;
        }
        return StringUtils.equals(string,"my-bucket");
    }

    private synchronized void cleanup() {
        LOG.info("----- started cleanup script -----");
        LOG.info("check unfinished users");

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
        LOG.info("----- ended cleanup script -----");
    }

}

