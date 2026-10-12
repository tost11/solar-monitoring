package de.tostsoft.solarmonitoring.app.startup;

import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.testlib.startup.BaseStartupMailSentTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SolarmonitoringApplication.class)
@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "fulldomain=http://localhost:8050"})
class StartupMailSentTest extends BaseStartupMailSentTest {

    @Override
    protected String getAppName() {
        return "Backend";
    }

    @Override
    protected String getDomain() {
        return "http://localhost:8050";
    }
}
