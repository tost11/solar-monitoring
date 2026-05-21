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

    @Value("${threaded.poolsize.recalculateStatistics.min:0}")
    private int recalculateStatisticsMinThreadPoolSize;

    @Value("${threaded.poolsize.recalculateStatistics:5}")
    private int recalculateStatisticsMaxThreadPoolSize;

    @Bean
    public ThreadPoolExecutor recalculateStatisticsThreadPool() {
        return new ThreadPoolExecutor(
                recalculateStatisticsMinThreadPoolSize, // core pool size: default 0 (no idle threads)
                recalculateStatisticsMaxThreadPoolSize, // maximum pool size: default 5
                60, TimeUnit.SECONDS, // keep-alive for idle threads
                new java.util.concurrent.LinkedBlockingQueue<>(2000), // Queue up to 2000 tasks
                new ThreadPoolExecutor.AbortPolicy() // Reject with exception when full - NEVER BLOCKS CALLER
        );
    }




}
