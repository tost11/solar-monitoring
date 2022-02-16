package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.dtos.grafana.GrafanaUserDTO;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.Instant;
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
  private GrafanaService grafanaService;

  @PostConstruct
  public void runInitialCleanup(){
    cleanup();
  }

  @Scheduled(cron="0 1 * * * *")
  public void runDailyCleanup(){
    cleanup();
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
    while(hasNext){
      var grafanaUsers = grafanaService.getGrafanaUsers(page, size);
      for (GrafanaUserDTO grafanaUser : grafanaUsers) {
        //TODO parse username and check in neo4j for existence and delete if not found, and delete (keep in mind when deleteing here paging is not working)
      }
       hasNext = grafanaUsers.size() == size;
      page++;
    }

    ///TODO do the same for folder

    //TODO do the same for buckets also check if buckets are empty for extra security

    LOG.info("----- ended cleanup script -----");
  }

}
