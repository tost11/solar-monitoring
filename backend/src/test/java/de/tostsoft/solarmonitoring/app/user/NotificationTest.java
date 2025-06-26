package de.tostsoft.solarmonitoring.app.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.users.CreateNotificationDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.NotificationDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.NotificationType;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Collections;

public class NotificationTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    void testNotificationsLoadInSystem() throws JsonProcessingException {
        var user = addUser(true);

        var system = addSolarSystemForUser(user,SolarSystemType.GRID);

        var jwt = signIn();

        CreateNotificationDTO createNotificationDTO = new CreateNotificationDTO();
        createNotificationDTO.setValue("test@localhost");
        createNotificationDTO.setType(NotificationType.Mail);
        createNotificationDTO.setId(system.getId());

        var res = doRestRequest("api/user/notification",createNotificationDTO, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var notificationDTO = objectMapper.readValue(res.getBody(), NotificationDTO.class);

        var notification = notificationRepository.findById(notificationDTO.getId()).orElseThrow();

        user = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(user.getNotifications()).hasSize(1);
        Assertions.assertThat(user.getNotifications().get(0).getId()).isEqualTo(notification.getId());

        system = solarSystemRepository.findById(system.getId()).orElseThrow();
        Assertions.assertThat(system.getNotifier()).hasSize(1);
        Assertions.assertThat(system.getNotifier().get(0).getId()).isEqualTo(notification.getId());
    }
}
