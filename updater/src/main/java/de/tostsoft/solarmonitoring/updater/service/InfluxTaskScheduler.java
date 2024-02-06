package de.tostsoft.solarmonitoring.updater.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxTaskScheduler{

    @Autowired
    private InfluxTaskService influxTaskService;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    private static final Logger LOG = LoggerFactory.getLogger(InfluxTaskScheduler.class);

    @Scheduled(fixedDelayString = "${timing.updateDayData:900000}",initialDelayString = "${timing.delayDayData:0}")//check every 15 minutes
    public void updateDayData(){

        LOG.info("Running updateDayData scheduler (every 15 min)");

        //ZonedDateTime before = ZonedDateTime.ofInstant(calendar.toInstant(),ZoneId.of("UTC"));

        var list = solarSystemRepository.findAllByLastCalculationIsNull();
        for (var solarSystem : list) {
            influxTaskService.runInitial(solarSystem, solarSystem.getLastCalculation() != null ? ZonedDateTime.ofInstant(Instant.ofEpochMilli(solarSystem.getLastCalculation()), ZoneId.of(solarSystem.getTimezone())):null);
        }

        var before = ZonedDateTime.now();
        before = before.minusDays(2).minusHours(22);

        list = solarSystemRepository.findAllByLastCalculationIsLessThan(before.toInstant().toEpochMilli());
        for (var solarSystem : list) {
            influxTaskService.runInitial(solarSystem, ZonedDateTime.ofInstant(Instant.ofEpochMilli(solarSystem.getLastCalculation()),ZoneId.of(solarSystem.getTimezone())));
        }
    }


    //TODO move to microservice
    @Scheduled( fixedDelay = 60*1000*5,initialDelay = 1000 * 20)
    public void updateStatistics(){

        //TODO paging
        var solarSystems = solarSystemRepository.findAllByNeedsStatisticRecalculation(true);
        for (SolarSystem solarSystem : solarSystems) {
            try {
                var zId = ZoneId.of(solarSystem.getTimezone() == null ? "UTC" : solarSystem.getTimezone());
                var today = ZonedDateTime.now(zId);
                today = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
                influxTaskService.runUpdateLastDays(solarSystem, today);
                var yesterday = today.minusDays(1);
                influxTaskService.runUpdateLastDays(solarSystem, yesterday);
                influxTaskService.runUpdateTotalValues(solarSystem);
            }catch (Exception exception){
                LOG.error("Exception on processing last two day solar statistic update");
                exception.printStackTrace();
            }
        }
    }

}
