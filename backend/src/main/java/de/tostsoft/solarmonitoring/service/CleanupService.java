package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
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
  public void runInitialCleanup(){
    cleanup();
  }

  @Scheduled(cron="0 1 * * * *")
  public void runDailyCleanup(){
    cleanup();
  }

  private boolean checkUserPattern(String string){
    Pattern p = Pattern.compile("^user-\\d+");
    Matcher m = p.matcher(string);
    return m.matches();
  }
  private boolean checkDashboardPattern(String string){
    Pattern p = Pattern.compile("^dashboard-\\d+");
    Matcher m = p.matcher(string);
    return m.matches();
  }

  @Synchronized
  private void cleanup(){
    LOG.info("----- started cleanup script -----");
    LOG.info("check unfinished users");
    var time = Instant.now().minusSeconds(60*10);
    //var time = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));//every creation 10 minutes behind


    var users = userRepository.findAllNotInitializedAndCratedBefore(time);
    LOG.info("Found {} users in neo4j to cleanup",users.size());
    userRepository.deleteAll(users);
    LOG.info("Deleted {} users in neo4j",users.size());
    var systems = solarSystemRepository.findAllNotInitializedAndCratedBefore(time);
    LOG.info("Found {} systems in neo4j to cleanup",systems.size());
    solarSystemRepository.deleteAll(systems);
    LOG.info("Deleted {} systems in neo4j",systems.size());



    int page = 0;
    int size = 100;
    boolean hasNext = true;
    ArrayList<Long> toDeleteUsersID= new ArrayList<>();
    ArrayList<String> toDeleteFolderUID= new ArrayList<>();
    ArrayList<String> toDeleteBucket=new ArrayList<>();
    ArrayList<String> toDeleteDashboardsUid = new ArrayList<>();
    while(hasNext){
      var grafanaUsers = grafanaService.getGrafanaUsers(page, size);
      for (GrafanaUserDTO grafanaUser : grafanaUsers) {
        //TODO parse username and check in neo4j for existence and delete if not found, and delete (keep in mind when deleteing here paging is not working)

        if(checkUserPattern(grafanaUser.getLogin())){
          long userId= Long.parseLong(grafanaUser.getLogin().split("-")[1]);
          User user = userRepository.findById(userId);
          if (user == null) {
            toDeleteUsersID.add(grafanaUser.getId());
          }
        }
      }
       hasNext = grafanaUsers.size() == size;
      page++;
    }
    for (long grafanaUserId :toDeleteUsersID){
      grafanaService.deleteUser(grafanaUserId);

    }

    //Get from Folder all Names dell all witch id isn't in Neo4j
    var grafanaFolders = grafanaService.getFolders();
    for (GrafanaFoldersDTO grafanaFolder : Objects.requireNonNull(grafanaFolders.getBody())) {
      if (checkUserPattern(grafanaFolder.getUid())) {
        long userId = Long.parseLong(grafanaFolder.getUid().split("-")[1]);
        User user = userRepository.findById(userId);
        if (user == null) {
          toDeleteFolderUID.add(grafanaFolder.getUid());
        }
        else {
          var folder = grafanaService.getDashboardsByFolderId(grafanaFolder.getId());
          for (GrafanaDashboardDTO dashboardDTO : Objects.requireNonNull(folder.getBody())) {
            if (checkDashboardPattern(dashboardDTO.getUid())) {
              long solarSystemId = Long.parseLong(dashboardDTO.getUid().split("-")[1]);
              SolarSystem system = solarSystemRepository.findById(solarSystemId);
              if (system == null) {
                toDeleteDashboardsUid.add(dashboardDTO.getUid());
              }
            }

          }
        }
      } else {
        LOG.error("Folder has the wrong Pattern");
      }
    }
    for (String grafanaFolderID :toDeleteFolderUID){
      grafanaService.deleteFolder(grafanaFolderID);

    }
    for (String grafanaDashboardUID  :toDeleteDashboardsUid){
      grafanaService.deleteDashboard(grafanaDashboardUID);

    }
    //TODO do the same for buckets also check if buckets are empty for extra security
    List<Bucket> buckets = influxConnection.getBuckets();
    for (Bucket bucket :buckets){
      if(checkUserPattern(bucket.getName())){
        long userId= Long.parseLong(bucket.getName().split("-")[1]);
        User user = userRepository.findById(userId);
        if(user==null){
          toDeleteBucket.add(bucket.getName());
        }
      }
      else{
        LOG.error("Folder has the wrong Pattern");
      }
      for(String bucketName : toDeleteBucket){
        if(bucketName!=null) {
          influxConnection.deleteBucket(bucketName);
        }
        }

    }

    LOG.info("----- ended cleanup script -----");
  }

}
