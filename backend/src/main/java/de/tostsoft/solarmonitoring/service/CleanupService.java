package de.tostsoft.solarmonitoring.service;

import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaFoldersDTO;
import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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
import org.springframework.web.server.ResponseStatusException;

@EnableScheduling
@Service
public class CleanupService {

  private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

  @Autowired
  private UserRepository userRepository;

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

  private boolean checkPattern(String string){
    Pattern p = Pattern.compile("^user-\\d+");
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


    int page = 0;
    int size = 100;
    boolean hasNext = true;
    ArrayList<Long> toDeleteUsersID= new ArrayList<>();
    ArrayList<String> toDeleteFolderUID= new ArrayList<>();
    ArrayList<String> toDeleteBucket=new ArrayList<>();
    while(hasNext){
      var grafanaUsers = grafanaService.getGrafanaUsers(page, size);
      for (GrafanaUserDTO grafanaUser : grafanaUsers) {
        //TODO parse username and check in neo4j for existence and delete if not found, and delete (keep in mind when deleteing here paging is not working)

        if(checkPattern(grafanaUser.getLogin())){
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
    for (GrafanaFoldersDTO grafanaFolder : Objects.requireNonNull(grafanaFolders.getBody())){
      if(checkPattern(grafanaFolder.getUid())){
        long userId= Long.parseLong(grafanaFolder.getUid().split("-")[1]);
        User user = userRepository.findById(userId);
        if(user==null){
          toDeleteFolderUID.add(grafanaFolder.getUid());
        }
      }
      else{
        LOG.error("Folder has the wrong Pattern");
      }
      for (String grafanaFolderID :toDeleteFolderUID){
        grafanaService.deleteFolder(grafanaFolderID);

      }




    }

    //TODO do the same for buckets also check if buckets are empty for extra security
    List<Bucket> buckets = influxConnection.getBuckets();
    for (Bucket bucket :buckets){
      if(checkPattern(bucket.getName())){
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
