package de.tostsoft.solarmonitoring.testlib.BaseTests;

import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class BaseActuatorTest extends BaseRestTest {

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
