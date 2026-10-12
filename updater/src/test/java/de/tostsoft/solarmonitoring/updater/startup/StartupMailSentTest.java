package de.tostsoft.solarmonitoring.updater.startup;

import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailSentTest;
import de.tostsoft.solarmonitoring.updater.SolarmonitoringUpdater;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SolarmonitoringUpdater.class)
@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "fulldomain=http://localhost:8051"})
class StartupMailSentTest extends BaseStartupMailSentTest {

    @Override
    protected String getAppName() {
        return "Updater";
    }

    @Override
    protected String getDomain() {
        return "http://localhost:8051";
    }
}
