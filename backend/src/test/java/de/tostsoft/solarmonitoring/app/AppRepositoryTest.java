package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.testlib.BaseTests.BaseRepositoryTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

//this test class ist not empty it runs tests from base class
@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppRepositoryTest extends BaseRepositoryTest {

    public AppRepositoryTest(ApplicationContext context) {
        super(context);
    }

}
