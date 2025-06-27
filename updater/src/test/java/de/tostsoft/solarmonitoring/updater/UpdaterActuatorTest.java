package de.tostsoft.solarmonitoring.updater;

import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import de.tostsoft.solarmonitoring.testlib.BaseTests.BaseActuatorTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureObservability //needed for actuator to load prometheus endpoint
@SpringBootTest(classes = {SolarmonitoringUpdater.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdaterActuatorTest extends BaseActuatorTest {

    @LocalManagementPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }
}