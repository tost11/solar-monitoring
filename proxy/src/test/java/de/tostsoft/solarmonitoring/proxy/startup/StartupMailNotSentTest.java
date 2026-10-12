package de.tostsoft.solarmonitoring.proxy.startup;

import de.tostsoft.solarmonitoring.proxy.SolarMonitoringProxy;
import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailNotSentTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SolarMonitoringProxy.class)
class StartupMailNotSentTest extends BaseStartupMailNotSentTest {
}
