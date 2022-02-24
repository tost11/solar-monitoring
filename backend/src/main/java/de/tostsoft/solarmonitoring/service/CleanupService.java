package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaDashboardDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaFoldersDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private GrafanaService grafanaService;
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
        var time = Instant.now().minusSeconds(60 * 10);
        //var time = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));//every creation 10 minutes behind


        var users = userRepository.findAllNotInitializedAndCratedBefore(time);
        LOG.info("Found {} users in neo4j to cleanup", users.size());
        userRepository.deleteAll(users);
        LOG.info("Deleted {} users in neo4j", users.size());
        var systems = solarSystemRepository.findAllNotInitializedAndCratedBefore(time);
        LOG.info("Found {} systems in neo4j to cleanup", systems.size());
        solarSystemRepository.deleteAll(systems);
        LOG.info("Deleted {} systems in neo4j", systems.size());


        LOG.info("Check grafana users to be deleted");
        int page = 0;
        int size = 100;
        boolean hasNext = true;
        ArrayList<Long> toDeleteUsersID = new ArrayList<>();
        ArrayList<String> toDeleteFolderUID = new ArrayList<>();
        ArrayList<String> toDeleteBucket = new ArrayList<>();
        ArrayList<String> toDeleteDashboardsUid = new ArrayList<>();
        while (hasNext) {
            var grafanaUsers = grafanaService.getGrafanaUsers(page, size);
            for (GrafanaUserDTO grafanaUser : grafanaUsers) {
                if (!checkUserPattern(grafanaUser.getLogin())) {
                    LOG.debug("Grfana User has the wrong User Pattern and will be skipped {}",grafanaUser.getName());
                    continue;//user not matching user generated names pattern
                }
                long userId = Long.parseLong(grafanaUser.getLogin().split("-")[1]);
                User user = userRepository.findById(userId);
                if (user != null) {
                    continue;
                }
                toDeleteUsersID.add(grafanaUser.getId());
            }
            hasNext = grafanaUsers.size() == size;
            page++;
        }
        LOG.info("Found {} Grafana users to be deleted", toDeleteUsersID.size());
        for (long grafanaUserId : toDeleteUsersID) {
            LOG.info("Delete Grafana User " + grafanaUserId);
            grafanaService.deleteUser(grafanaUserId);
        }
        LOG.info("Deleted {} Grafana users", toDeleteUsersID.size());

        //Get from Folder all Names dell all witch id isn't in Neo4j
        var grafanaFolders = grafanaService.getFolders();
        for (GrafanaFoldersDTO grafanaFolder : Objects.requireNonNull(grafanaFolders.getBody())) {
            if (!checkUserPattern(grafanaFolder.getUid())) {
                LOG.debug("Grafana Folder has the wrong User Pattern and will be skipped {}",grafanaFolder.getUid());
                continue;
            }
            long userId = Long.parseLong(grafanaFolder.getUid().split("-")[1]);
            User user = userRepository.findUserById(userId);
            if (user == null) {
                toDeleteFolderUID.add(grafanaFolder.getUid());
            } else {
                //now check all dashboards in folder (representing systems)
                var folder = grafanaService.getDashboardsByFolderId(grafanaFolder.getId());
                for (GrafanaDashboardDTO dashboardDTO : Objects.requireNonNull(folder.getBody())) {
                    if (!checkDashboardPattern(dashboardDTO.getUid())) {
                        continue;
                    }
                    long solarSystemId = Long.parseLong(dashboardDTO.getUid().split("-")[1]);
                    SolarSystem system = solarSystemRepository.findById(solarSystemId);
                    if (system == null) {
                        toDeleteDashboardsUid.add(dashboardDTO.getUid());
                    }
                }
            }
        }
        LOG.info("Found {} Grafana folders to be deleted", toDeleteUsersID.size());
        for (String grafanaFolderID : toDeleteFolderUID) {
            LOG.info("Delete GrafanaFolder " + grafanaFolderID);
            grafanaService.deleteFolder(grafanaFolderID);
        }
        LOG.info("Deleted {} Grafana folders", toDeleteUsersID.size());
        LOG.info("Found {} Grafana dashboards to be deleted", toDeleteUsersID.size());
        for (String grafanaDashboardUID : toDeleteDashboardsUid) {
            LOG.info("Delete GrafanaDashboard " + grafanaDashboardUID);
            grafanaService.deleteDashboard(grafanaDashboardUID);
        }
        LOG.info("Deleted {} Grafana dashboards", toDeleteUsersID.size());

        List<Bucket> buckets = influxConnection.getBuckets();
        for (Bucket bucket : buckets) {
            if (!checkUserPattern(bucket.getName())) {
                LOG.debug("Bucket not matching user pattern skip it {}",bucket.getName());
                continue;
            }
            long userId = Long.parseLong(bucket.getName().split("-")[1]);
            User user = userRepository.findUserById(userId);
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

