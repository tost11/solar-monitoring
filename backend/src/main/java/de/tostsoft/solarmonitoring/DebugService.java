package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.dtos.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarIfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.GrafanaService;
import de.tostsoft.solarmonitoring.service.SolarService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class DebugService implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(SolarService.class);
    private List<Thread> threads = new ArrayList<>();
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private SolarSystemService solarSystemService;
    @Autowired
    private UserService userService;

    @Value("${debug.token:}")
    private String debugToken;
    @Value("${debug.username}")
    private String username;
    @Value("${debug.password}")
    private String password;
    @Value("${debug.system}")
    private String system;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    SolarSystemRepository solarSystemRepository;

    private void addSystem(User user,SolarSystemType type){
        String name = system+" "+type;
        LOG.info("Create debug system: {}",name);
        var response = solarSystemService.createSystemForUser(new RegisterSolarSystemDTO(name,type),user);
        var system = solarSystemRepository.findById(response.getId()).get();
        system.setToken(passwordEncoder.encode(debugToken));
        solarSystemRepository.save(system);
    }

    private User crateTestUserWithSystem() {
        LOG.info("Try to create debug test user: {}",username);

        var user = userRepository.findByNameIgnoreCase(username);
        if(user.isPresent()){
            LOG.info("Test user already exists using that one");
            return user.get();
        }

        userService.registerUser(new UserRegisterDTO(username,password));

        user = userRepository.findByNameIgnoreCase(username);

        //create systems
        addSystem(user.get(),SolarSystemType.SELFMADE);
        addSystem(user.get(),SolarSystemType.SELFMADE_INVERTER);
        addSystem(user.get(),SolarSystemType.SELFMADE_CONSUMPTION);
        addSystem(user.get(),SolarSystemType.SELFMADE_DEVICE);

        LOG.info("Debug data created");
        return user.get();
    }

    private float lerp(float a, float b, float f) {
        return (a * (1.0f - f)) + (b * f);
    }

    public SelfMadeSolarIfluxPoint updateTestData(SelfMadeSolarIfluxPoint lastTestData,int iteration){
        if (lastTestData == null) {
            lastTestData = SelfMadeSolarIfluxPoint.builder()
                    .duration(10000.f)
                    .chargeVolt(20.f)
                    .chargeAmpere(2.f)
                    .chargeWatt(40.f)
                    .batteryVoltage(12.f)
                    .batteryAmpere(1.333f)
                    .batteryWatt(16.f)
                    .batteryPercentage(null)
                    .batteryTemperature(15.f)
                    .consumptionDeviceVoltage(12.f)
                    .consumptionDeviceAmpere(2.f)
                    .consumptionDeviceWatt(24.f)
                    .consumptionInverterVoltage(230.f)
                    .consumptionInverterAmpere(0.1f)
                    .consumptionInverterWatt(230.f*0.1f)
                    .inverterTemperature(10.5f)
                    .deviceTemperature(15.f)
                    .totalConsumption(24.f+230.f*0.1f).build();
        } else {

            //solar data
            float value = (float) (0.5 * Math.random());
            if (Math.random() > 0.5) {
                value = value * -1;
            }
            value = lastTestData.getChargeVolt() + value;
            value = Math.min(Math.max(16, value), 40);
            lastTestData.setChargeVolt(value);
            if (iteration % 10 == 0) {
                float val = lastTestData.getChargeAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.2 : Math.random() * -0.2);
                val = Math.min(Math.max(0, val), 10);
                lastTestData.setDeviceTemperature(val);
            }
            lastTestData.setChargeWatt(lastTestData.getChargeVolt() * lastTestData.getChargeAmpere());

            value = lerp(10, 14, 0.5f + ((lastTestData.getChargeWatt() - lastTestData.getTotalConsumption()) / (40 * 2)));

            lastTestData.setBatteryVoltage(value);
            lastTestData.setConsumptionDeviceVoltage(value);
            value = lastTestData.getConsumptionDeviceAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.25f : Math.random() * -0.25f);
            value = Math.min(Math.max(0, value), 10);
            lastTestData.setConsumptionDeviceAmpere(value);
            lastTestData.setConsumptionDeviceWatt(lastTestData.getConsumptionDeviceAmpere() * lastTestData.getConsumptionDeviceVoltage());

            lastTestData.setBatteryWatt(lastTestData.getChargeWatt() - lastTestData.getConsumptionDeviceWatt());
            lastTestData.setBatteryAmpere(lastTestData.getBatteryWatt() / lastTestData.getBatteryAmpere());

            if (iteration % 100 == 0) {
                float val = lastTestData.getBatteryTemperature() + (float) (Math.random() > 0.5 ? Math.random() : Math.random() * -1);
                val = Math.min(Math.max(-20, val), 40);
                lastTestData.setBatteryTemperature(val);
            }

            lastTestData.setChargeTemperature(lastTestData.getBatteryTemperature() + lastTestData.getChargeWatt() / 200.f);
            lastTestData.setTotalConsumption(lastTestData.getConsumptionDeviceWatt()+lastTestData.getConsumptionInverterWatt());

            lastTestData.setTimestamp(new Date().getTime());
        }

        lastTestData.setTimestamp(new Date().getTime());
        lastTestData.setDuration(10.f);
        lastTestData.setType(SolarSystemType.SELFMADE);
        return lastTestData;
    }


    @Override
    public void run(String... args) {
        LOG.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        for (String arg : args) {
            if (arg.equals("debug")) {
                LOG.info("Runnig in debug mode");

                var user = crateTestUserWithSystem();
                long id = user.getId();

                var thread = new Thread(() -> {
                    var system = solarSystemRepository.findAllByTypeAndRelationOwnedById(SolarSystemType.SELFMADE,id).get(0);
                    int i = 0;
                    SelfMadeSolarIfluxPoint selfMadeSolarIfluxPoint = null;
                    while (true) {
                        selfMadeSolarIfluxPoint = updateTestData(selfMadeSolarIfluxPoint,i);
                        SelfMadeSolarIfluxPoint copy = selfMadeSolarIfluxPoint.copy();
                        copy.setTotalConsumption(null);
                        copy.setConsumptionDeviceVoltage(null);
                        copy.setConsumptionDeviceAmpere(null);
                        copy.setConsumptionDeviceWatt(null);
                        copy.setConsumptionInverterVoltage(null);
                        copy.setConsumptionInverterAmpere(null);
                        copy.setConsumptionInverterWatt(null);
                        copy.setBatteryTemperature(null);
                        copy.setType(SolarSystemType.SELFMADE);
                        copy.setSystemId(system.getId());
                        influxConnection.newPoint(system,copy);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                        if (i > 100) {
                            i = 0;
                        }
                    }
                });
                thread.start();
                threads.add(thread);

                thread = new Thread(() -> {
                    var system = solarSystemRepository.findAllByTypeAndRelationOwnedById(SolarSystemType.SELFMADE_CONSUMPTION,id).get(0);
                    int i = 0;
                    SelfMadeSolarIfluxPoint selfMadeSolarIfluxPoint = null;
                    while (true) {
                        selfMadeSolarIfluxPoint = updateTestData(selfMadeSolarIfluxPoint,i);
                        SelfMadeSolarIfluxPoint copy = selfMadeSolarIfluxPoint.copy();
                        copy.setType(SolarSystemType.SELFMADE_CONSUMPTION);
                        copy.setSystemId(system.getId());
                        influxConnection.newPoint(system,copy);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                        if (i > 100) {
                            i = 0;
                        }
                    }
                });
                thread.start();
                threads.add(thread);

                thread = new Thread(() -> {
                    var system = solarSystemRepository.findAllByTypeAndRelationOwnedById(SolarSystemType.SELFMADE_INVERTER,id).get(0);
                    int i = 0;
                    SelfMadeSolarIfluxPoint selfMadeSolarIfluxPoint = null;
                    while (true) {
                        selfMadeSolarIfluxPoint = updateTestData(selfMadeSolarIfluxPoint,i);
                        var copy = selfMadeSolarIfluxPoint.copy();
                        copy.setTotalConsumption(selfMadeSolarIfluxPoint.getTotalConsumption()-selfMadeSolarIfluxPoint.getConsumptionDeviceWatt());
                        copy.setConsumptionDeviceVoltage(null);
                        copy.setConsumptionDeviceAmpere(null);
                        copy.setConsumptionDeviceWatt(null);
                        copy.setType(SolarSystemType.SELFMADE_INVERTER);
                        copy.setSystemId(system.getId());
                        influxConnection.newPoint(system,copy);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                        if (i > 100) {
                            i = 0;
                        }
                    }
                });
                thread.start();
                threads.add(thread);

                thread = new Thread(() -> {
                    var system = solarSystemRepository.findAllByTypeAndRelationOwnedById(SolarSystemType.SELFMADE_DEVICE,id).get(0);
                    int i = 0;
                    SelfMadeSolarIfluxPoint selfMadeSolarIfluxPoint = null;
                    while (true) {
                        selfMadeSolarIfluxPoint = updateTestData(selfMadeSolarIfluxPoint,i);
                            var copy = selfMadeSolarIfluxPoint.copy();
                            copy.setTotalConsumption(selfMadeSolarIfluxPoint.getTotalConsumption()-selfMadeSolarIfluxPoint.getConsumptionDeviceWatt());
                            copy.setConsumptionInverterVoltage(null);
                            copy.setConsumptionInverterAmpere(null);
                            copy.setConsumptionInverterWatt(null);
                            copy.setInverterTemperature(null);
                            copy.setSystemId(system.getId());
                            copy.setType(SolarSystemType.SELFMADE_DEVICE);
                            influxConnection.newPoint(system,copy);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                        if (i > 100) {
                            i = 0;
                        }
                    }
                });
                thread.start();
                threads.add(thread);
            }
        }
    }
}
