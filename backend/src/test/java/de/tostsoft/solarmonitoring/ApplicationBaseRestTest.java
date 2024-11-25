package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

public class ApplicationBaseRestTest extends BaseRestTest {

    @LocalServerPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }
}
