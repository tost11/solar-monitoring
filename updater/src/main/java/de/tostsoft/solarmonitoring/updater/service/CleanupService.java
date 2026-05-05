package de.tostsoft.solarmonitoring.updater.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    @Autowired
    private RegisterUserRepository registerUserRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Value("${configNode:root}")
    private String configName;

    //default two weeks
    @Value("${timing.usersKeptDeleted:1209600000}")
    private long usersKeptDeleted;
    @Autowired
    private ManagesRepository managesRepository;
    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private JWTSessionTokenRepository jwtSessionTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private AccountLockoutRepository accountLockoutRepository;

    @Autowired
    private de.tostsoft.solarmonitoring.lib.service.MailService mailService;

    @Value("${monitoring.mail:#{null}}")
    private String monitoringMail;

    public final static int DELTE_USERS_PAGE_SIZE = 20;

    @Scheduled(cron = "${timing.dailyCleanup:0 1 * * * *}")
    public void runDailyCleanup() {
        LOG.info("----- started daily cleanup script -----");

        try{
            checkUnfinishedUsers();
        }catch (Exception e){
            LOG.error("Error checking for data of unfinished users",e);
        }

        try{
            resetDailyRegistrations();
        }catch (Exception e){
            LOG.error("Error while resetting daily registrations",e);
        }

        try{
            checkTTLIndexHealth();
        }catch (Exception e){
            LOG.error("Error checking MongoDB TTL index health",e);
        }

        LOG.info("----- ended daily cleanup script -----");
    }

    @Scheduled(fixedDelayString = "${timing.continuousCleanup.delay:300000}", initialDelayString = "${timing.continuousCleanup.start:0}")
    public void runContinousCleanup() {
        LOG.info("----- started continous cleanup script -----");

        // Captcha, RegisterUser, and JWTSessionToken cleanup now handled by MongoDB TTL indexes
        // TTL indexes automatically delete expired documents:
        // - Captcha: expires after 1 hour (3600 seconds)
        // - RegisterUser: expires after 24 hours (86400 seconds)
        // - JWTSessionToken: expires at exact validUntil timestamp (expireAfterSeconds = 0)

        try{
            realDeleteUsersAndData();
        }catch (Exception e){
            LOG.error("Error while deleting users",e);
        }

        LOG.info("----- ended continuous cleanup script -----");
    }

    public boolean isPreservedName(String string) {
        if(StringUtils.startsWith(string,"_")){
            return true;
        }
        return StringUtils.equals(string,"my-bucket");
    }

    public void checkUnfinishedUsers(){
        LOG.info("-> check unfinished users");

        ArrayList<String> toDeleteBucket = new ArrayList<>();

        List<Bucket> buckets = influxConnection.getBuckets();
        for (Bucket bucket : buckets) {

            if (!isPreservedName(bucket.getName())) {
                LOG.debug("Bucket is preserved: {}",bucket.getName());
                continue;
            }
            var userOpt = userRepository.findByInfluxBucketNameWithDeleted(bucket.getName());
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

    public void resetDailyRegistrations(){
        LOG.info("-> reset daily registrations");

        configRepository.resetDailyRegistrations(configName);

        LOG.info("daily registrations were reset");
    }

    public void realDeleteUsersAndData(){

        var deleteTimestamp = LocalDateTime.now(ZoneId.of("UTC"));
        deleteTimestamp = deleteTimestamp.minus(usersKeptDeleted,ChronoUnit.MILLIS);

        boolean more = true;
        while(more){
            var res = userRepository.findAllByDeletedAtBefore(deleteTimestamp,Pageable.ofSize(DELTE_USERS_PAGE_SIZE));
            more = res.hasNext();

            for (User user : res) {
                LOG.info("Delete user {} with id: {}",user.getName(),user.getId());

                for (Manages mange : user.getManges()) {
                    managesRepository.deleteById(mange.getId());
                }
                for (SolarSystem system : user.getOwns()) {
                    for (Manages manages : system.getManagedBy()) {
                        managesRepository.deleteById(manages.getId());
                    }
                    solarSystemRepository.deleteById(system.getId());
                }
                userRepository.deleteById(user.getId());
            }
        }
    }

    public void checkTTLIndexHealth() {
        LOG.info("-> check MongoDB TTL index health");

        Instant now = Instant.now();
        int bufferSeconds = 900; // 15 minutes buffer
        List<String> warnings = new ArrayList<>();

        // Check Captcha (TTL: 3600 seconds = 1 hour)
        Instant captchaThreshold = now.minus(3600 + bufferSeconds, ChronoUnit.SECONDS);
        long staleCaptchas = captchaRepository.countByCreatedAtBefore(captchaThreshold.toEpochMilli());
        if (staleCaptchas > 0) {
            String warning = String.format("Found %d Captcha entities that should have been deleted by TTL (older than %d minutes)",
                    staleCaptchas, (3600 + bufferSeconds) / 60);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Check RegisterUser (TTL: 86400 seconds = 24 hours)
        Instant registerUserThreshold = now.minus(86400 + bufferSeconds, ChronoUnit.SECONDS);
        long staleRegisterUsers = registerUserRepository.countByCreatedAtBefore(registerUserThreshold.toEpochMilli());
        if (staleRegisterUsers > 0) {
            String warning = String.format("Found %d RegisterUser entities that should have been deleted by TTL (older than %d hours)",
                    staleRegisterUsers, (86400 + bufferSeconds) / 3600);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Check JWTSessionToken (TTL: 0 seconds = expire at exact timestamp)
        Instant jwtThreshold = now.minus(bufferSeconds, ChronoUnit.SECONDS);
        long staleJWTTokens = jwtSessionTokenRepository.countByValidUntilBefore(jwtThreshold);
        if (staleJWTTokens > 0) {
            String warning = String.format("Found %d JWTSessionToken entities that should have been deleted by TTL (expired over %d minutes ago)",
                    staleJWTTokens, bufferSeconds / 60);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Check PasswordResetToken (TTL: 3600 seconds = 1 hour)
        Instant resetTokenThreshold = now.minus(3600 + bufferSeconds, ChronoUnit.SECONDS);
        long staleResetTokens = passwordResetTokenRepository.countByExpiresAtBefore(resetTokenThreshold);
        if (staleResetTokens > 0) {
            String warning = String.format("Found %d PasswordResetToken entities that should have been deleted by TTL (older than %d minutes)",
                    staleResetTokens, (3600 + bufferSeconds) / 60);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Check LoginAttempt (TTL: 86400 seconds = 1 day)
        Instant loginAttemptThreshold = now.minus(86400 + bufferSeconds, ChronoUnit.SECONDS);
        long staleLoginAttempts = loginAttemptRepository.countByTimestampBefore(loginAttemptThreshold);
        if (staleLoginAttempts > 0) {
            String warning = String.format("Found %d LoginAttempt entities that should have been deleted by TTL (older than %d hours)",
                    staleLoginAttempts, (86400 + bufferSeconds) / 3600);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Check AccountLockout (TTL: 604800 seconds = 7 days)
        Instant accountLockoutThreshold = now.minus(604800 + bufferSeconds, ChronoUnit.SECONDS);
        long staleAccountLockouts = accountLockoutRepository.countByExpiresAtBefore(accountLockoutThreshold);
        if (staleAccountLockouts > 0) {
            String warning = String.format("Found %d AccountLockout entities that should have been deleted by TTL (older than %d days)",
                    staleAccountLockouts, (604800 + bufferSeconds) / 86400);
            LOG.warn(warning);
            warnings.add(warning);
        }

        // Send alert email if issues found
        if (!warnings.isEmpty()) {
            LOG.error("MongoDB TTL index health check FAILED - {} entity types have stale documents", warnings.size());

            if (monitoringMail != null && mailService.isMailConfigured()) {
                StringBuilder emailBody = new StringBuilder();
                emailBody.append("MongoDB TTL Index Health Check Alert\n");
                emailBody.append("=====================================\n\n");
                emailBody.append("The following issues were detected:\n\n");

                for (String warning : warnings) {
                    emailBody.append("- ").append(warning).append("\n");
                }

                emailBody.append("\nThis indicates that MongoDB's TTL background thread may not be functioning correctly.\n");
                emailBody.append("Please check:\n");
                emailBody.append("1. MongoDB server is running and accessible\n");
                emailBody.append("2. TTL indexes are properly created (check with db.collection.getIndexes())\n");
                emailBody.append("3. MongoDB TTL monitor thread is enabled (default: runs every 60 seconds)\n");
                emailBody.append("4. MongoDB server logs for errors related to TTL index processing\n");

                try {
                    mailService.sendMail(monitoringMail,
                            "ALERT: MongoDB TTL Index Health Check Failed",
                            emailBody.toString());
                    LOG.info("Sent TTL health check alert email to {}", monitoringMail);
                } catch (Exception e) {
                    LOG.error("Failed to send TTL health check alert email", e);
                }
            } else {
                LOG.warn("TTL health check alert email not sent - monitoring.mail not configured or mail service unavailable");
            }
        } else {
            LOG.info("MongoDB TTL index health check passed - all entity types are being cleaned up correctly");
        }
    }

}

