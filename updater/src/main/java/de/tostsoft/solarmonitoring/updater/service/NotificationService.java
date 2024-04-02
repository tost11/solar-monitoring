package de.tostsoft.solarmonitoring.updater.service;

import de.tostsoft.solarmonitoring.lib.model.Notification;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    private Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private MailService mailService;

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

    private boolean isOnlineCheck(SolarSystem solarSystem){
        if(solarSystem.getType() == SolarSystemType.SELFMADE){
            int checkSeconds = 30 * 3;
            if(solarSystem.getViewData() != null && solarSystem.getViewData().getDefaultDelay() != null){
                checkSeconds = solarSystem.getViewData().getDefaultDelay() * 3;
            }
            return solarSystem.isOnline(checkSeconds);
        }else{
            //TODO

            return false;
        }
    }

    private void checkForNotification(SolarSystem solarSystem){
        boolean status = isOnlineCheck(solarSystem);
        if(status != solarSystem.getLastOnlineCheckStatus()){
            solarSystemRepository.updateLastOnlineCheckStatus(solarSystem.getId(),status);
            for (Notification notification : solarSystem.getNotifier()) {
                notify(solarSystem,status,notification);
            }
        }
    }

    @Scheduled(fixedDelayString = "${timing.updateDayData:900000}",initialDelayString = "${timing.delayDayData:0}")//check 15 every minutes
    public void checkForSendingNotifications(){

        Pageable pageableRequest = PageRequest.of(0, 20);
        //iteration over all systems needed because specific query to find only notifications systems is not working because of mongo limitations
        //totally hours wasted here 6
        Page<SolarSystem> page = solarSystemRepository.findAll(pageableRequest);
        while(page.getSize() > 0){
            for (SolarSystem solarSystem : page) {
                checkForNotification(solarSystem);
            }
        }

    }
}
