package de.tostsoft.solarmonitoring.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfiguration {

    @Value("${threaded.poolsize.caculateafterwards:5}")
    private int calculateAfterwardsPoolSize;

    @Bean
    public ThreadPoolTaskScheduler solarDataCalculationAfterwardsExecutor() {

        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setAcceptTasksAfterContextClose(true);
        threadPoolTaskScheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
        threadPoolTaskScheduler.setAwaitTerminationMillis(30 * 1000);
        threadPoolTaskScheduler.setPoolSize(calculateAfterwardsPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix(
                "solar-data-calculate-afterwards-");
        return threadPoolTaskScheduler;
    }

}
