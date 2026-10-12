package de.tostsoft.solarmonitoring.updater.startup;

import de.tostsoft.solarmonitoring.testlib.startup.BaseNoDomainStartupTest;
import de.tostsoft.solarmonitoring.updater.SolarmonitoringUpdater;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SolarmonitoringUpdater.class)
@TestPropertySource(properties = {"monitoring.mail=testuser@local.host", "fulldomain="})
class NoDomainStartupTest extends BaseNoDomainStartupTest {

    @Override
    protected String getAppName() {
        return "Updater";
    }
}
