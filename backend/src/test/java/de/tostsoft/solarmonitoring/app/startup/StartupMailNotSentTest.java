package de.tostsoft.solarmonitoring.app.startup;

import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailNotSentTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SolarmonitoringApplication.class)
class StartupMailNotSentTest extends BaseStartupMailNotSentTest {
}
