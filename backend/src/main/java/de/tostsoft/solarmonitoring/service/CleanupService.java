package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
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

    private boolean checkUserPattern(String string) {
        Pattern p = Pattern.compile("^user-\\d+");
        Matcher m = p.matcher(string);
        return m.matches();
    }

    private boolean checkDashboardPattern(String string) {
        Pattern p = Pattern.compile("^dashboard-\\d+");
        Matcher m = p.matcher(string);
        return m.matches();
    }

    @Synchronized
    private void cleanup() {
        LOG.info("----- started cleanup script -----");
        LOG.info("check unfinished users");

        ArrayList<String> toDeleteBucket = new ArrayList<>();

        List<Bucket> buckets = influxConnection.getBuckets();
        for (Bucket bucket : buckets) {
            if (!checkUserPattern(bucket.getName())) {
                LOG.debug("Bucket not matching user pattern skip it {}",bucket.getName());
                continue;
            }
            long userId = Long.parseLong(bucket.getName().split("-")[1]);
            User user = userRepository.findById(userId);
            if (user == null) {
                toDeleteBucket.add(bucket.getName());
            }
        }
        LOG.info("Found {} Influx buckets to be deleted", toDeleteBucket.size());
        for (String bucketName : toDeleteBucket) {
            if (bucketName != null) {
                LOG.info("Delete Influx Bucket " + bucketName);
                influxConnection.deleteBucket(bucketName);
            }
        }
        LOG.info("Deleted {} Influx buckets", toDeleteBucket.size());
        LOG.info("----- ended cleanup script -----");
    }

}

