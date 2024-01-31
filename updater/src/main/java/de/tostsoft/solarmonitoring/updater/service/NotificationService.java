package de.tostsoft.solarmonitoring.updater.service;

import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    SolarSystemRepository solarSystemRepository;

    private void checkForNotification(SolarSystem solarSystem){

    }

    /*@Scheduled(fixedDelayString = "${timing.updateDayData:300000}",initialDelayString = "${timing.delayDayData:0}")//check 5 every minutes
    public void checkForSendingNotifications(){

        Pageable pageableRequest = PageRequest.of(0, 20);
        Page<SolarSystem> page = solarSystemRepository.findAll(pageableRequest);
        while(page.getSize() > 0){
            for (SolarSystem solarSystem : page) {
                checkForNotification(solarSystem);
            }
        }

    }*/
}
