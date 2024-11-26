package de.tostsoft.solarmonitoring.proxy;

import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.springframework.boot.test.web.server.LocalServerPort;

public class ProxyBaseRestTest extends BaseRestTest {

    @LocalServerPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }
}
