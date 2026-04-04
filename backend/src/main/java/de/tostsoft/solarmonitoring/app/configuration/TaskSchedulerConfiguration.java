package de.tostsoft.solarmonitoring.app.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Value("${threaded.poolsize.recalculateStatistics:5}")
    private int recalculateStatisticsMaxThreadPoolSize;

    @PostConstruct
    public ThreadPoolExecutor recalculateStatisticsThreadPool() {
        return new ThreadPoolExecutor(
                0, // core pool size: 0
                recalculateStatisticsMaxThreadPoolSize, // maximum pool size
                30, TimeUnit.SECONDS, // idle threads timeout
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.CallerRunsPolicy() // fallback policy
        );
    }




}
