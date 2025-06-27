package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.testlib.BaseTests.BaseActuatorTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureObservability //needed for actuator to load prometheus endpoint
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppActuatorTest extends BaseActuatorTest {

    @LocalManagementPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }
}