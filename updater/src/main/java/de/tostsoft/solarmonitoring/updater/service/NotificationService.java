package de.tostsoft.solarmonitoring.updater.service;

import de.tostsoft.solarmonitoring.lib.model.Notification;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    private Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private MailService mailService;

    private int minDistanceSecondsForNotAvailable = 10 * 60;

    @Autowired
    private Environment env;

    private boolean isSampleInRange(String bucket, String systemId,ZonedDateTime start,ZonedDateTime end){

        StringBuilder query = new StringBuilder(512);
        query.append("from(bucket: \"").append(bucket).append("\")\n")
                .append("  |> range(start: ").append(zoneFormatter.format(start)).append(", stop: ").append(zoneFormatter.format(end)).append(")\n")
                .append("  |> filter(fn: (r) => r[\"_measurement\"] == \"solar-data\")\n")
                .append("  |> filter(fn: (r) => r[\"_field\"] == \"InputWatt\")\n")
                .append("  |> filter(fn: (r) => r[\"system\"] == \"").append(systemId).append("\")\n")
                .append("  |> first()\n\n");

        var res = influxConnection.getClient().getQueryApi().query(query.toString());
        return !res.isEmpty();
    }

    private Duration calcCheckSeconds(SolarSystem solarSystem){
        int checkSeconds = minDistanceSecondsForNotAvailable;
        /*if(solarSystem.getViewData() != null && solarSystem.getViewData().getDefaultDelay() != null){
            int sec = solarSystem.getViewData().getDefaultDelay() * 3;
            if(sec > minDistanceSecondsForNotAvailable){
                checkSeconds = sec;
            }
        }*/
        return Duration.ofSeconds(checkSeconds);
    }

    private boolean checkNoBatteryWasSystemOnline(SolarSystem solarSystem){


        var now = ZonedDateTime.now();
        var distance = calcCheckSeconds(solarSystem);
        var start = now.minus(Duration.ofDays(1)).minus(Duration.ofMinutes(15)).minus(distance);
        var end = now.minus(Duration.ofDays(1)).minus(Duration.ofMinutes(15));
        return isSampleInRange(solarSystem.getOwnedBy().getInfluxBucketName(),solarSystem.getInfluxTagName(),start,end);
    }

    private boolean checkNoBatteryIsSystemOnline(SolarSystem solarSystem){
        Duration checkSeconds = calcCheckSeconds(solarSystem);
        return solarSystem.isOnline(checkSeconds.multipliedBy(3));
    }

    void sendMail(SolarSystem solarSystem, boolean status,String mail){
        if(StringUtils.isEmpty(mail)){
            logger.error("Could not send status mail because target mail is emtpy");
            return;
        }
        mailService.sendMail(mail,
                "SolarSystem "+(status ? "Online":"Offline"),
                status ? "Congratulations your solar System: "+solarSystem.getViewName()+" is (back) online":
                "Oh no it seems something went wrong your Solar System: "+solarSystem.getViewName()+" is offline -.-");
    }

    void notify(SolarSystem solarSystem,boolean status,Notification notification){
        if(notification.getType() == NotificationType.Mail){
            sendMail(solarSystem,status,notification.getValue());
        }else if(notification.getType() == NotificationType.UserMail){
            sendMail(solarSystem,status,notification.getUser().getMail());
        }else{
            logger.error("Notification for type logger not implemented: "+notification.getType());
        }
    }

    private void checkForNotification(SolarSystem solarSystem){
        if(solarSystem.getLastOnlineCheckStatus() == null){
            solarSystemRepository.updateLastOnlineCheckStatus(solarSystem.getId(),solarSystem.isOnline());
            return;
        }
        if(solarSystem.getType() == SolarSystemType.SELFMADE || solarSystem.getType() == SolarSystemType.GRID_BATTERY){
            var distance = calcCheckSeconds(solarSystem);
            boolean status = solarSystem.isOnline(distance);
            if(status != solarSystem.getLastOnlineCheckStatus()){
                solarSystemRepository.updateLastOnlineCheckStatus(solarSystem.getId(),status);
                for (Notification notification : solarSystem.getNotifier()) {
                    notify(solarSystem,status,notification);
                }
            }
        }else{
            boolean wasOnlineYesterday = checkNoBatteryWasSystemOnline(solarSystem);
            boolean isOnline = solarSystem.isOnline();
            boolean wasOnline = solarSystem.getLastOnlineCheckStatus();
            if(isOnline != wasOnline){
                if(isOnline){
                    solarSystemRepository.updateLastOnlineCheckStatus(solarSystem.getId(),true);
                    for (Notification notification : solarSystem.getNotifier()) {
                        notify(solarSystem,true,notification);
                    }
                }else if(wasOnlineYesterday){
                    solarSystemRepository.updateLastOnlineCheckStatus(solarSystem.getId(),false);
                    for (Notification notification : solarSystem.getNotifier()) {
                        notify(solarSystem,false,notification);
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${timing.updateDayData:100000}",initialDelayString = "${timing.delayDayData:0}")
    public void checkForSendingNotifications(){

        logger.info("Notification Service update");

        Pageable pageableRequest = PageRequest.of(0, 20);
        //iteration over all systems needed because specific query to find only notifications systems is not working because of mongo limitations
        //totally hours wasted here
        Page<SolarSystem> page = solarSystemRepository.findAll(pageableRequest);
        while(true){
            for (SolarSystem solarSystem : page) {
                checkForNotification(solarSystem);
            }
            if(!page.hasNext()){
                break;
            }
            page = solarSystemRepository.findAll(page.nextPageable());
        }

    }
}
