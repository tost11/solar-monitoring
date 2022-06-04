package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.controller.data.GridSolarController;
import de.tostsoft.solarmonitoring.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.DeviceGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.SimpleGridSolarSampleDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridInputDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.grid.helper.GridOutputDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarInfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.service.InfluxTaskService;
import de.tostsoft.solarmonitoring.service.SolarService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.UserService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Profile("debug")
public class DebugService{
    private static final Logger LOG = LoggerFactory.getLogger(SolarService.class);
    private List<Thread> threads = new ArrayList<>();
    @Autowired
    private InfluxConnection influxConnection;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemService solarSystemService;
    @Autowired
    private UserService userService;

    @Autowired
    private InfluxTaskService influxTaskService;

    @Autowired
    private GridSolarController gridSolarController;

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

    public void addSystem(User user,SolarSystemType type){
        String name = system+" "+type;
        LOG.info("Create debug system: {}",name);
        var response = solarSystemService.createSystemForUser(RegisterSolarSystemDTO.builder().name(name).type(type).maxSolarVoltage(60).timezone(TimeZone.getDefault().getID()).build(),user);
        var system = solarSystemRepository.findById(response.getId()).get();
        system.setToken(passwordEncoder.encode(debugToken));
        solarSystemRepository.save(system);
    }

    public User crateTestUserWithSystem() {
        LOG.info("Try to create debug test user: {}",username);

        var user = userRepository.findByNameIgnoreCase(username);
        if(user!=null){

            LOG.info("Test user already exists using that one");
            return user;
        }

        userService.registerUser(new UserRegisterDTO(username,password));

        user = userRepository.findByNameIgnoreCase(username);
        user.setIsAdmin(true);
        user.setNumAllowedSystems(100);
        user = userRepository.save(user);

        //create systems
        addSystem(user,SolarSystemType.SELFMADE);
        addSystem(user,SolarSystemType.SELFMADE_INVERTER);
        addSystem(user,SolarSystemType.SELFMADE_CONSUMPTION);
        addSystem(user,SolarSystemType.SELFMADE_DEVICE);
        addSystem(user,SolarSystemType.SIMPLE);
        addSystem(user,SolarSystemType.VERY_SIMPLE);
        addSystem(user,SolarSystemType.GRID);
        addSystem(user,SolarSystemType.GRID);

        user = userRepository.findById(user.getId()).get();
        LOG.info("Debug data created");
        return user;
    }

    private float lerp(float a, float b, float f) {
        return (a * (1.0f - f)) + (b * f);
    }

    public SelfMadeSolarInfluxPoint updateTestData(SelfMadeSolarInfluxPoint lastTestData,int iteration){
        if (lastTestData == null) {
            lastTestData = SelfMadeSolarInfluxPoint.builder()
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
                    .inverterFrequency(50.f)
                    .totalConsumption(24.f+230.f*0.1f).build();
            lastTestData.setDuration(10000.f);
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


    @PostConstruct
    public void init() {
        LOG.info("Runnig in debug mode");

        var user = crateTestUserWithSystem();

        influxTaskService.runAllInitialTasks();

        long id = user.getId();

        /*for (SolarSystem solarSystem : solarSystemRepository.findAll()) {
            influxTaskService.deleteAllDayData(solarSystem);
        }*/

        var thread = new Thread(() -> {
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.SELFMADE, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                SelfMadeSolarInfluxPoint copy = selfMadeSolarInfluxPoint.copy();
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
                influxConnection.newPoint(system, copy);
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
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.SELFMADE_CONSUMPTION, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                SelfMadeSolarInfluxPoint copy = selfMadeSolarInfluxPoint.copy();
                copy.setType(SolarSystemType.SELFMADE_CONSUMPTION);
                if( i%10 == 0){
                    copy.setConsumptionInverterAmpere(null);
                    copy.setConsumptionInverterVoltage(null);
                    copy.setConsumptionInverterWatt(null);
                }
                copy.setSystemId(system.getId());
                influxConnection.newPoint(system, copy);
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
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.SELFMADE_INVERTER, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                var copy = selfMadeSolarInfluxPoint.copy();
                copy.setTotalConsumption(
                    selfMadeSolarInfluxPoint.getTotalConsumption()
                        - selfMadeSolarInfluxPoint.getConsumptionDeviceWatt());
                copy.setConsumptionDeviceVoltage(null);
                copy.setConsumptionDeviceAmpere(null);
                copy.setConsumptionDeviceWatt(null);
                if( i%10 == 0){
                    copy.setConsumptionInverterAmpere(null);
                    copy.setConsumptionInverterVoltage(null);
                    copy.setConsumptionInverterWatt(null);
                }
                copy.setType(SolarSystemType.SELFMADE_INVERTER);
                copy.setSystemId(system.getId());
                influxConnection.newPoint(system, copy);
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
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.SELFMADE_DEVICE, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                var copy = selfMadeSolarInfluxPoint.copy();
                copy.setTotalConsumption(selfMadeSolarInfluxPoint.getTotalConsumption()
                    - selfMadeSolarInfluxPoint.getConsumptionDeviceWatt());
                copy.setConsumptionInverterVoltage(null);
                copy.setConsumptionInverterAmpere(null);
                copy.setConsumptionInverterWatt(null);
                copy.setInverterTemperature(null);
                copy.setSystemId(system.getId());
                copy.setType(SolarSystemType.SELFMADE_DEVICE);
                influxConnection.newPoint(system, copy);
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

        // ---------------------- simple ---------------------------

        thread = new Thread(() -> {
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.SIMPLE, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                SelfMadeSolarInfluxPoint copy = selfMadeSolarInfluxPoint.copy();
                copy.setTotalConsumption(null);
                copy.setConsumptionDeviceVoltage(null);
                copy.setConsumptionDeviceAmpere(null);
                copy.setConsumptionDeviceWatt(null);
                copy.setConsumptionInverterVoltage(null);
                copy.setConsumptionInverterAmpere(null);
                copy.setConsumptionInverterWatt(null);
                copy.setBatteryTemperature(null);
                copy.setType(SolarSystemType.SIMPLE);
                copy.setSystemId(system.getId());
                influxConnection.newPoint(system, copy);
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
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.VERY_SIMPLE, id).get(0);
            int i = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint = null;
            while (true) {
                selfMadeSolarInfluxPoint = updateTestData(selfMadeSolarInfluxPoint, i);
                SelfMadeSolarInfluxPoint copy = selfMadeSolarInfluxPoint.copy();
                copy.setChargeAmpere(null);
                copy.setChargeVolt(null);
                copy.setTotalConsumption(null);
                copy.setConsumptionDeviceVoltage(null);
                copy.setConsumptionDeviceAmpere(null);
                copy.setConsumptionDeviceWatt(null);
                copy.setConsumptionInverterVoltage(null);
                copy.setConsumptionInverterAmpere(null);
                copy.setConsumptionInverterWatt(null);
                copy.setBatteryTemperature(null);
                copy.setType(SolarSystemType.VERY_SIMPLE);
                copy.setSystemId(system.getId());
                influxConnection.newPoint(system, copy);
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
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.GRID, id).get(0);
            int i1 = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint1 = null;
            while (true) {
                selfMadeSolarInfluxPoint1 = updateTestData(selfMadeSolarInfluxPoint1, i1);

                double diff = 36000000.;
                double tempTotalKWH = new Date().getTime();
                tempTotalKWH /= diff;
                tempTotalKWH -= 45900.;
                float totalKWH = (float)tempTotalKWH;

                var dto = SimpleGridSolarSampleDTO.builder()
                    .chargeVoltage(selfMadeSolarInfluxPoint1.getChargeVolt()*10)
                    .chargeAmpere(selfMadeSolarInfluxPoint1.getChargeAmpere())
                    .gridVoltage(selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .gridAmpere(selfMadeSolarInfluxPoint1.getChargeWatt() / selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .totalKWH(totalKWH)
                    .frequency(50.f)
                    .phase(1)
                    .duration(10.f).build();

                gridSolarController.PostDataSimple(system.getId(),dto,debugToken);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i1++;
                if (i1 > 100) {
                    i1 = 0;
                }
            }
        });
        thread.start();
        threads.add(thread);

        thread = new Thread(() -> {
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.GRID, id).get(0);
            int i1 = 0;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint1 = null;
            while (true) {
                selfMadeSolarInfluxPoint1 = updateTestData(selfMadeSolarInfluxPoint1, i1);

                double diff = 36000000.;
                double tempTotalKWH = new Date().getTime();
                tempTotalKWH /= diff;
                tempTotalKWH -= 45900.;
                float totalKWH = (float)tempTotalKWH;

                var dto = SimpleGridSolarSampleDTO.builder()
                    .chargeVoltage(selfMadeSolarInfluxPoint1.getChargeVolt()*10)
                    .chargeAmpere(selfMadeSolarInfluxPoint1.getChargeAmpere())
                    .gridVoltage(selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .gridAmpere(selfMadeSolarInfluxPoint1.getChargeWatt() / selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .frequency(50.f)
                    .phase(1)
                    .totalKWH(totalKWH)
                    .duration(10.f).build();

                gridSolarController.PostDataSimple(system.getId(),dto,debugToken);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i1++;
                if (i1 > 100) {
                    i1 = 0;
                }
            }
        });
        thread.start();
        threads.add(thread);

        thread = new Thread(() -> {
            var system = solarSystemRepository.findAllByTypeAndRelationOwnedByIdWithOwnerRelation(
                SolarSystemType.GRID, id).get(1);
            int i1 = 0;
            int i2 = 5;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint1 = null;
            SelfMadeSolarInfluxPoint selfMadeSolarInfluxPoint2 = null;

            while (true) {

                double diff = 36000000.;
                double tempTotalKWH = new Date().getTime();
                tempTotalKWH /= diff;
                tempTotalKWH -= 45900.;
                float totalKWH1 = (float)tempTotalKWH;
                float totalKWH2 = (float)tempTotalKWH * 0.5f;

                selfMadeSolarInfluxPoint1 = updateTestData(selfMadeSolarInfluxPoint1, i1);
                selfMadeSolarInfluxPoint2 = updateTestData(selfMadeSolarInfluxPoint2, i2);

                var input1Dto = GridInputDTO.builder()
                    .id(1L)
                    .voltage(selfMadeSolarInfluxPoint1.getChargeVolt()*10)
                    .ampere(selfMadeSolarInfluxPoint1.getChargeAmpere())
                    .build();
                var output1Dto = GridOutputDTO.builder()
                    .id(1L)
                    .voltage(selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .ampere(selfMadeSolarInfluxPoint1.getChargeVolt()*10 * selfMadeSolarInfluxPoint1.getChargeAmpere() / selfMadeSolarInfluxPoint1.getConsumptionInverterVoltage())
                    .phase(1)
                    .frequency(50.f)
                    .build();
                var device1DTO = GridDeviceDTO.builder()
                    .id(1L)
                    .inputs(Arrays.asList(input1Dto))
                    .outputs(Arrays.asList(output1Dto))
                    .deviceTemperature(selfMadeSolarInfluxPoint1.getDeviceTemperature())
                    .totalKWH(totalKWH1)
                    .build();

                var input2Dto = GridInputDTO.builder()
                    .id(1L)
                    .voltage(selfMadeSolarInfluxPoint2.getChargeVolt()*10)
                    .ampere(selfMadeSolarInfluxPoint2.getChargeAmpere()/2)
                    .build();
                var input3Dto = GridInputDTO.builder()
                    .id(2L)
                    .voltage(selfMadeSolarInfluxPoint2.getChargeVolt()*10)
                    .ampere(selfMadeSolarInfluxPoint2.getChargeAmpere()/2)
                    .build();
                var output2Dto = GridOutputDTO.builder()
                    .id(1L)
                    .voltage(selfMadeSolarInfluxPoint2.getConsumptionInverterVoltage())
                    .ampere(selfMadeSolarInfluxPoint2.getChargeVolt()*10 * selfMadeSolarInfluxPoint2.getChargeAmpere()/ selfMadeSolarInfluxPoint2.getConsumptionInverterVoltage())
                    .phase(2)
                    .frequency(49.5f)
                    .build();

                var device2DTO = GridDeviceDTO.builder()
                    .id(2L)
                    .inputs(Arrays.asList(input2Dto,input3Dto))
                    .outputs(Arrays.asList(output2Dto))
                    .deviceTemperature(selfMadeSolarInfluxPoint2.getDeviceTemperature())
                    .totalKWH(totalKWH2)
                    .build();

                var deviceGridSolarSampleDTO = DeviceGridSolarSampleDTO.builder()
                    .devices(Arrays.asList(device1DTO,device2DTO))
                    .duration(10.f)
                    .build();

                gridSolarController.PostDevice(system.getId(),deviceGridSolarSampleDTO,debugToken);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i1++;
                if (i1 > 100) {
                    i1 = 0;
                }
                i2++;
                if (i2 > 100) {
                    i2 = 0;
                }
            }
        });
        thread.start();
        threads.add(thread);
    }
}
