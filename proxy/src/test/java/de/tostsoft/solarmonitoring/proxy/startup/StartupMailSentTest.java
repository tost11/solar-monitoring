package de.tostsoft.solarmonitoring.proxy.startup;

import de.tostsoft.solarmonitoring.proxy.SolarMonitoringProxy;
import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailSentTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SolarMonitoringProxy.class)
@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "fulldomain=http://localhost:8052"})
class StartupMailSentTest extends BaseStartupMailSentTest {

    @Override
    protected String getAppName() {
        return "Proxy";
    }

    @Override
    protected String getDomain() {
        return "http://localhost:8052";
    }
}
