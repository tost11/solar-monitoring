package de.tostsoft.solarmonitoring.updater.startup;

import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailNotSentTest;
import de.tostsoft.solarmonitoring.updater.SolarmonitoringUpdater;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SolarmonitoringUpdater.class)
class StartupMailNotSentTest extends BaseStartupMailNotSentTest {
}
