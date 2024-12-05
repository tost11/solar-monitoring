package de.tostsoft.solarmonitoring.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.app.controller.SolarDataController;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.BatteryDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.DeviceDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.InputACDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.InputDCDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.OutputACDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.OutputDCDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.app.service.*;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Profile("debug")
public class DebugService{
    private static final Logger LOG = LoggerFactory.getLogger(SolarService.class);
    private List<Thread> threads = new ArrayList<>();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SolarSystemService solarSystemService;
    @Autowired
    private UserService userService;
    @Autowired
    private SolarDataController solarController;

    @Value("${debug.token:}")
    private String debugToken;
    @Value("${debug.username}")
    private String username;
    @Value("${debug.password}")
    private String password;
    @Value("${debug.system}")
    private String system;
    @Value("${debug.autoinit}")
    private boolean autoinit;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    SolarSystemRepository solarSystemRepository;

    public void addSystem(User user,SolarSystemType type){
        addSystem(user,type, system+" "+type);
    }

    public void addSystem(User user,SolarSystemType type,String name){
        LOG.info("Create debug system: {}",name);
        var response = solarSystemService.createSystemForUser(RegisterSolarSystemDTO.builder()
                        .name(name)
                        .type(type)
                        .maxSolarVoltage(60)
                        .viewData(new ViewDataDTO())
                        .timezone(TimeZone.getDefault().getID())
                        .publicMode(PublicMode.ALL).build(),
            user);
        var system = solarSystemRepository.findById(response.getId()).get();
        system.setToken(passwordEncoder.encode(debugToken));
        solarSystemRepository.save(system);
    }

    public User crateTestUserWithSystem(SolarSystemType type) {
        LOG.info("Try to create debug test user: {}",username);

        var user = userRepository.findByName(StringUtils.lowerCase(username));
        if(user!=null){
            LOG.info("Test user already exists using that one");
            return user;
        }

        userService.registerUser(new UserRegisterDTO(username,password));

        user = userRepository.findByName(StringUtils.lowerCase(username));
        user.setIsAdmin(true);
        user.setNumAllowedSystems(100);
        user = userRepository.save(user);

        //create systems
        if(type == null) {
            addSystem(user, SolarSystemType.SELFMADE);
            addSystem(user, SolarSystemType.SIMPLE);
            addSystem(user, SolarSystemType.VERY_SIMPLE);
            addSystem(user, SolarSystemType.GRID);
            addSystem(user, SolarSystemType.GRID_BATTERY);
            addSystem(user, SolarSystemType.GRID_BATTERY);

            //TODO add deye serial
            addSystem(user, SolarSystemType.GRID,"five min push system");
        }else{
            addSystem(user, type);
        }

        user = userRepository.findById(user.getId()).get();
        LOG.info("Debug data created");
        return user;
    }

    private float lerp(float a, float b, float f) {
        return (a * (1.0f - f)) + (b * f);
    }

    public SampleDTO updateTestData(SampleDTO lastTestData, int iteration){
        if (lastTestData == null) {
            lastTestData = SampleDTO.builder()
                    .inputVoltageDC(20.f)
                    .inputAmpereDC(2.f)
                    .inputWattDC(40.f)
                    .batteryVoltage(12.f)
                    .batteryAmpere(1.333f)
                    .batteryWatt(16.f)
                    .batteryPercentage(null)
                    .batteryTemperature(15.f)
                    .outputVoltageDC(230.f)
                    .outputAmpereDC(0.1f)
                    .outputWattDC(230.f*0.1f)
                    .temperature(10.5f)
                    .batteryTemperature(15.f)
                    .outputFrequency(50.f).build();
            lastTestData.setDuration(10000.f);
        } else {

            //solar data
            float value = (float) (0.5 * Math.random());
            if (Math.random() > 0.5) {
                value = value * -1;
            }
            value = lastTestData.getInputVoltageDC() + value;
            value = Math.min(Math.max(16, value), 40);
            lastTestData.setInputVoltageDC(value);
            if (iteration % 10 == 0) {
                float val = lastTestData.getTemperature() + (float) (Math.random() > 0.5 ? Math.random() * 0.2 : Math.random() * -0.2);
                val = Math.min(Math.max(0, val), 10);
                lastTestData.setTemperature(val);
            }
            lastTestData.setInputWattDC(lastTestData.getInputVoltageDC() * lastTestData.getInputAmpereDC());

            value = lerp(10, 14, 0.5f + ((lastTestData.getInputWattDC() - lastTestData.getOutputWattDC()) / (40 * 2)));

            lastTestData.setBatteryVoltage(value);
            lastTestData.setOutputVoltageDC(value);
            value = lastTestData.getOutputAmpereDC() + (float) (Math.random() > 0.5 ? Math.random() * 0.25f : Math.random() * -0.25f);
            value = Math.min(Math.max(0, value), 10);
            lastTestData.setOutputAmpereDC(value);
            lastTestData.setOutputWattDC(lastTestData.getOutputAmpereDC() * lastTestData.getOutputVoltageDC());

            lastTestData.setBatteryWatt(lastTestData.getInputWattDC() - lastTestData.getOutputWattDC());
            lastTestData.setBatteryAmpere(lastTestData.getBatteryWatt() / lastTestData.getBatteryAmpere());

            if (iteration % 100 == 0) {
                float val = lastTestData.getBatteryTemperature() + (float) (Math.random() > 0.5 ? Math.random() : Math.random() * -1);
                val = Math.min(Math.max(-20, val), 40);
                lastTestData.setBatteryTemperature(val);
            }

            lastTestData.setTemperature(lastTestData.getBatteryTemperature() + lastTestData.getInputWattDC() / 200.f);
            float lastTotal = lastTestData.getInputTotalKWH() == null ? 0 : lastTestData.getInputTotalKWH();
            lastTestData.setInputTotalKWH(lastTestData.getInputWattDC() + lastTotal);

            lastTestData.setTimestamp(new Date().getTime());
        }

        lastTestData.setTimestamp(new Date().getTime());
        lastTestData.setDuration(10.f);

        return lastTestData;
    }

    private void randomizeInput(InputDCDTO dto){
        float value = dto.getVoltage() + (float) (Math.random() - 0.5f);
        value = Math.min(Math.max(16, value), 40);
        dto.setVoltage(value);

        value = dto.getAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.25f : Math.random() * -0.25f);
        value = Math.min(Math.max(0, value), 10);
        dto.setAmpere(value);

        dto.setWatt(dto.getVoltage()*dto.getAmpere());
    }

    private void randomizeInput(InputACDTO dto){
        float value = dto.getVoltage () + (float) (0.001 * (Math.random()-0.5f));
        value = Math.min(Math.max(220, value), 240);
        dto.setVoltage(value);

        value = dto.getAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.025f : Math.random() * -0.025f);
        value = Math.min(Math.max(0, value), 0.5f);
        dto.setAmpere(value);

        dto.setWatt(dto.getVoltage()*dto.getAmpere());
    }

    private void randomizeOutput(OutputDCDTO dto,Float voltage){
        float value = voltage + (float) (0.1 * (Math.random()-0.5f));
        value = Math.min(10f,Math.max(14.5f,value));
        dto.setVoltage(value);

        value = dto.getAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.25f : Math.random() * -0.25f);
        value = Math.min(Math.max(0, value), 10);
        dto.setAmpere(value);

        dto.setWatt(dto.getVoltage()*dto.getAmpere());
    }

    private void randomizeOutput(OutputACDTO dto){
        float value = dto.getVoltage () + (float) (0.001 * (Math.random()-0.5f));
        if (Math.random() > 0.5) {
            value = value * -1;
        }
        value = dto.getVoltage() + value;
        value = Math.min(Math.max(220, value), 230);
        dto.setVoltage(value);

        value = dto.getAmpere() + (float) (Math.random() > 0.5 ? Math.random() * 0.025f : Math.random() * -0.025f);
        value = Math.min(Math.max(0, value), 3.f);
        dto.setAmpere(value);

        dto.setWatt(dto.getVoltage()*dto.getAmpere());
    }


    private void randomizeDevice(DeviceDTO dto,int iteration){

        if (iteration % 10 == 0) {
            float val = dto.getTemperature() + (float) (Math.random() > 0.5 ? Math.random() * 0.2 : Math.random() * -0.2);
            val = Math.min(Math.max(0, val), 10);
            dto.setTemperature(val);
        }
    }

    private float calculateBattery(DeviceDTO deviceDTO){
        float watt = 0.f;
        if(deviceDTO.getInputsDC() != null) {
            for (var d : deviceDTO.getInputsDC()) {
                watt += d.getWatt();
            }
        }
        if(deviceDTO.getInputsAC() != null) {
            for (var d : deviceDTO.getInputsAC()) {
                watt += d.getWatt();
            }
        }
        if(deviceDTO.getOutputsDC() != null) {
            for (var d : deviceDTO.getOutputsDC()) {
                watt -= d.getWatt();
            }
        }
        if(deviceDTO.getOutputsDC() != null) {
            for (var d : deviceDTO.getOutputsDC()) {
                watt -= d.getWatt();
            }
        }

        float volt = 12f + watt / 200;
        volt = Math.max(10,Math.min(14.5f,volt));

        var bat = BatteryDTO.builder()
            .id(1L)
            .voltage(volt)
            .watt(watt)
            .build();
        deviceDTO.setBatteries(List.of(bat));

        return volt;
    }

    public void updateDeviceKWHANDOHWithTime(List<DeviceDTO> devices){
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime birthday = LocalDateTime.of(2010, Month.JANUARY, 1,0,0);

        long seconds = ChronoUnit.SECONDS.between(birthday, today);
        float hours = ((float)seconds / (60*60));

        int i = 1;
        int o = 1;
        int j = 1;

        for (DeviceDTO device : devices) {
            if(device.getInputsAC() != null) {
                for (var v : device.getInputsAC()) {
                    v.setTotalKWH((hours + 10) * (i++ + 1) / 10);
                }
            }
            if(device.getInputsDC() != null) {
                for (var v : device.getInputsDC()) {
                    v.setTotalKWH((hours + 10) * (i++ + 1) / 10);
                }
            }
            if(device.getOutputsAC() != null) {
                for (var v : device.getOutputsAC()) {
                    v.setTotalKWH((hours + 10) * (o++ + 1) / 10);
                }
            }
            if(device.getOutputsDC() != null) {
                for (var v : device.getOutputsDC()) {
                    v.setTotalKWH((hours + 10) * (o++ + 1) / 10);
                }
            }

            device.setTotalOH((hours + 100)*(j+++1)/10);
        }
    }

    public SampleDTO updateTestDataInputAndOutput(SampleDTO lastTestData, int iteration){

        if (lastTestData == null) {

            DeviceDTO device1DTO = DeviceDTO.builder().id(1L).temperature(10.5f).build();

            InputDCDTO input1DTO = InputDCDTO.builder().id(1L)
                    .voltage(20.f)
                    .ampere(2.f)
                    .watt(40.f)
                    .build();

            InputDCDTO input2DTO = InputDCDTO.builder().id(2L)
                    .voltage(20.f)
                    .ampere(2.f)
                    .watt(40.f)
                    .build();

            device1DTO.setInputsDC(Arrays.asList(input1DTO,input2DTO));

            input1DTO = InputDCDTO.builder().id(1L)
                    .voltage(20.f)
                    .ampere(2.f)
                    .watt(40.f)
                    .build();

            var input1ACDTO = InputACDTO.builder().id(1L)
                    .voltage(230f)
                    .ampere(0.2f)
                    .watt(46.f)
                    .frequency(50f)
                    .phase(3)
                    .build();

            var output1DTO = OutputDCDTO.builder().id(1L)
                    .voltage(12.f)
                    .ampere(2.f)
                    .watt(24.f)
                    .build();

            var output2DTO = OutputDCDTO.builder().id(2L)
                    .voltage(12.f)
                    .ampere(1.f)
                    .watt(12.f)
                    .build();

            var outputACDTO = OutputACDTO.builder().id(1L)
                    .voltage(230f)
                    .ampere(0.2f)
                    .watt(46.f)
                    .frequency(49.75f)
                    .phase(1)
                    .build();

            var outputAC2DTO = OutputACDTO.builder().id(2L)
                    .voltage(230f)
                    .ampere(0.2f)
                    .watt(46.f)
                    .frequency(50.25f)
                    .phase(2)
                    .build();

            DeviceDTO device2DTO = DeviceDTO.builder().id(2L).temperature(8.5f).build();

            device2DTO.setInputsDC(List.of(input1DTO));
            device2DTO.setInputsAC(List.of(input1ACDTO));
            device2DTO.setOutputsDC(Arrays.asList(output1DTO,output2DTO));
            device2DTO.setOutputsAC(Arrays.asList(outputACDTO,outputAC2DTO));

            //calculsate battery stats
            float batteryVoltage = 12f;
            batteryVoltage += calculateBattery(device2DTO);
            batteryVoltage += calculateBattery(device1DTO);
            batteryVoltage /= 3;

            lastTestData = SampleDTO.builder()
                    .batteryVoltage(batteryVoltage)
                    //.batteryAmpere(1.333f)
                    //.batteryWatt(16.f)
                    //.batteryPercentage(null)
                    .batteryTemperature(15.f)
                    .build();

            lastTestData.setDuration(10000.f);

            lastTestData.setDevices(new ArrayList<>(List.of(device1DTO,device2DTO)));
            //lastTestData.setDevices(Arrays.asList(device1DTO));
            updateDeviceKWHANDOHWithTime(lastTestData.getDevices());
        } else {

            float totalWatt = 0;

            for (DeviceDTO device : lastTestData.getDevices()) {
                randomizeDevice(device,iteration);
                if(device.getInputsDC() != null) {
                    for (var input : device.getInputsDC()) {
                        randomizeInput(input);
                        totalWatt += input.getWatt();
                    }
                }
                if(device.getInputsAC() != null) {
                    for (var input : device.getInputsAC()) {
                        randomizeInput(input);
                        totalWatt += input.getWatt();
                    }
                }
                if(device.getOutputsDC() != null) {
                    for (var output : device.getOutputsDC()) {
                        randomizeOutput(output, lastTestData.getBatteryVoltage());
                        totalWatt = totalWatt - output.getWatt();
                    }
                }
                if(device.getOutputsAC() != null) {
                    for (var output : device.getOutputsAC()) {
                        randomizeOutput(output);
                        totalWatt = totalWatt - output.getWatt();
                    }
                }
            }

            int num = 1;
            float batteryVoltage = lastTestData.getBatteryVoltage();
            for (DeviceDTO device : lastTestData.getDevices()) {
                batteryVoltage += calculateBattery(device);
                num++;
            }
            batteryVoltage /= num;
            lastTestData.setBatteryVoltage(batteryVoltage);

            if (iteration % 100 == 0) {
                float val = lastTestData.getBatteryTemperature() + (float) (Math.random() > 0.5 ? Math.random() : Math.random() * -1);
                val = Math.min(Math.max(-20, val), 40);
                lastTestData.setBatteryTemperature(val);
            }
            updateDeviceKWHANDOHWithTime(lastTestData.getDevices());
        }

        lastTestData.setTimestamp(new Date().getTime());
        lastTestData.setDuration(10.f);

        return lastTestData;
    }

    public void startOnFirstSystemOfType(String userId, SolarSystemType type){
        var thread = new Thread(() -> {
            var system = solarSystemRepository.seesAllFindByTypeAndOwnedById(type, userId).get(0);
            int i = 0;
            SampleDTO sampleDTO = null;
            while (true) {
                sampleDTO = updateTestData(sampleDTO, i);

                try {
                    var restTemplate = new RestTemplate();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    headers.set("clientToken",debugToken);

                    sampleDTO.setTimestamp(System.currentTimeMillis());
                    //sampleDTO.setTimeUnit(TimeUnit.SECONDS);

                    String reqBodyData = new ObjectMapper().writeValueAsString(sampleDTO);
                    //System.out.println(reqBodyData);
                    var entity = new HttpEntity<>(reqBodyData, headers);
                    restTemplate.postForEntity("http://localhost:8052/api/solar/data?systemId="+system.getId(),entity,String.class);

                    //solarController.PostDevice(system.getId(), sampleDTO, debugToken);
                }catch (Exception ex){
                    System.out.println("Exception on post");
                    ex.printStackTrace();
                }

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

    @PostConstruct
    public void init() {

        //deye sun test code
        /*var s = solarSystemRepository.findById("65481b61228b0a5a12bc32ec");
        var set = new HashSet<Long>();
        set.add(4131146746L);
        s.get().setDeyeSunSerials(set);
        solarSystemRepository.save(s.get());*/



        //var mongoRes = mongoTestRepository.findByTestValue("epic_name");
        //if(mongoRes == null) {
        //    MongoTestObject t = new MongoTestObject(null, "epic_name");
        //    mongoTestRepository.save(t);
        //}

        //statusService.addStatus("test",true,2,0);
        //statusService.removeStatus("test2",2,0);

        //var r = statusController.getAllStatus(2,debugToken);

        //userRepository.deleteAll();
        //solarSystemRepository.deleteAll();

        LOG.info("Running in debug mode with autoinit: {}",autoinit);

        if(!autoinit){
            return;
        }

        var user = crateTestUserWithSystem(null);

        //influxTaskService.runAllInitialTasks();

        String id = user.getId();

        /*for (SolarSystem solarSystem : solarSystemRepository.findAll()) {
            influxTaskService.deleteAllDayData(solarSystem);
        }*/

        for (SolarSystemType value : SolarSystemType.values()) {
            startOnFirstSystemOfType(id,value);
        }

        var thread = new Thread(() -> {
            var system = solarSystemRepository.seesAllFindByTypeAndOwnedById(SolarSystemType.GRID, id).get(1);
            int i = 0;
            SampleDTO sampleDTO = null;
            while (true) {
                sampleDTO = updateTestData(sampleDTO, i);
                sampleDTO.setDuration(60.f * 5.f);

                try {
                    solarController.PostDevice(system.getId(), sampleDTO, debugToken);
                }catch (Exception ex){
                    System.out.println("Exception on post");
                }

                try {
                    Thread.sleep(1000 * 60 * 5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
                if (i > 100) {
                    i = 0;
                }
            }
        });

        //thread.start();
        //threads.add(thread);

        thread = new Thread(() -> {
            var system = solarSystemRepository.findByTypeAndOwnedById(SolarSystemType.GRID, id).get(1);
            int i = 0;
            SampleDTO sampleDTO = null;
            while (true) {
                sampleDTO = updateTestDataInputAndOutput(sampleDTO, i);

                while(sampleDTO.getDevices().size() > 1){
                    sampleDTO.getDevices().remove(1);
                }

                sampleDTO.getDevices().get(0).setBatteries(new ArrayList<>());

                //sampleDTO.setInputVoltage(0.f);
                //RestTemplate restTemplate = new RestTemplate();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                /*HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.set("clientToken",debugToken);

                var entity = new HttpEntity<>(sampleDTO, headers);
                restTemplate.postForEntity("http://localhost:8080/api/solar/data?systemId="+system.getId(),entity,String.class);*/

                var batVolt = sampleDTO.getBatteryVoltage();
                sampleDTO.setBatteryVoltage(null);
                //sampleDTO.setInputWattDC(0.f);
                //sampleDTO.setInputWatt(0.f);
                //sampleDTO.setInputAmpereDC(0.f);
                for (DeviceDTO device : sampleDTO.getDevices()) {
                    device.setInputsAC(null);
                    //device.setInputsDC(null);
                    //device.getInputsAC().clear();
                    //device.getInputsDC().clear();
                }

                sampleDTO.setTimestamp(null);
                sampleDTO.setDuration(60.f * 5);
                //test backwards compatibility
                try {

                    /*var restTemplate = new RestTemplate();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    headers.set("clientToken","123456789");

                    var entity = new HttpEntity<>(sampleDTO, headers);
                    restTemplate.postForEntity("http://localhost:8050/api/solar/data/deye?serialId=1234",entity,String.class);*/

                    solarController.PostDeviceDeye("1234", sampleDTO, "123456789");
                }catch (Exception ex){
                    ex.printStackTrace();
                    System.out.println("Exception on post deye");
                }
                sampleDTO.setBatteryVoltage(batVolt);

                try {
                    Thread.sleep(60000 * 5);
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

        /*
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
                    .inputVoltageage(selfMadeSolarInfluxPoint1.getChargeVolt()*10)
                    .inputAmpere(selfMadeSolarInfluxPoint1.getChargeAmpere())
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
                    .inputVoltageage(selfMadeSolarInfluxPoint1.getChargeVolt()*10)
                    .inputAmpere(selfMadeSolarInfluxPoint1.getChargeAmpere())
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

                var deviceGridSolarSampleDTO = GridSampleDTO.builder()
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
        threads.add(thread);*/
    }
}
