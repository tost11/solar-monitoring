package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.junit.jupiter.api.Test;
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
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ActuatorTest extends BaseRestTest {

    @LocalManagementPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }

    @ParameterizedTest
    @ValueSource(strings = {"health","info","prometheus"})
    public void checkActuatorPathsActive(String path) {
        assertThat(doRequest(path).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "beans","caches","conditions",
            "configprops","env","loggers",
            "heapdump","threaddump","metrics",
            "scheduledtasks","mappings"
    })
    public void checkActuatorPathsNotActive(String path) {
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest(path));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
