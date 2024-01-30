package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.Notification;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(NotificationType type, String value, SolarSystem solarSystem, User user){
        return notificationRepository.save(Notification.builder()
                .type(type)
                .value(value)
                .user(user)
                .solarSystem(solarSystem)
                .build());
    }
}
