package de.tostsoft.solarmonitoring.proxy.startup;

import de.tostsoft.solarmonitoring.proxy.SolarMonitoringProxy;
import de.tostsoft.solarmonitoring.testlib.startup.BaseNoDomainStartupTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SolarMonitoringProxy.class)
@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "fulldomain="})
class NoDomainStartupTest extends BaseNoDomainStartupTest {

    @Override
    protected String getAppName() {
        return "Proxy";
    }
}
