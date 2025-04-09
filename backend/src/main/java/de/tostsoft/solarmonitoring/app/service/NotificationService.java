package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.lib.model.Notification;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public void deleteNotification(User user, String id) {
        var not = notificationRepository.findById(id);
        if(not.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");
        }
        if(!not.get().getUser().getId().equals(user.getId())){
            //laso not found so user down know this id exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");
        }
        notificationRepository.delete(not.get());
    }
}
