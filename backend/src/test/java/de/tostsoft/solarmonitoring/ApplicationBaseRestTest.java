package de.tostsoft.solarmonitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.app.SolarmonitoringApplication;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.TimeZone;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationBaseRestTest extends BaseRestTest {

    @Autowired
    protected InfluxConnection influxConnection;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected SolarSystemRepository solarSystemRepository;

    @Autowired
    protected ManagesRepository managesRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @LocalServerPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected void clearDatabase() {

        for (Bucket bucket : influxConnection.getBuckets()) {
            if(bucket.getName().startsWith("_")){//skip system buckets
                continue;
            }
            influxConnection.deleteBucket(bucket.getName());
        }
        userRepository.deleteAll();
        solarSystemRepository.deleteAll();
        managesRepository.deleteAll();
        tagRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    protected User addUser(boolean admin){
        return addUser(admin,"test");
    }

    protected User addUser(boolean admin,String name){
        var user = User.builder()
                .name(name)
                .password(passwordEncoder.encode("password"))
                .isAdmin(admin)
                .creationDate(LocalDateTime.now())
                .numAllowedSystems(100)
                .viewName(name.toUpperCase())
                .influxBucketName(name)
                .build();

        influxConnection.createNewBucket(user.getInfluxBucketName());

        return userRepository.save(user);
    }

    protected SolarSystem addSolarSystemForUser(User user,SolarSystemType type){
        return addSolarSystemForUser(user,type,"test");
    }

    protected SolarSystem addSolarSystemForUser(User user,SolarSystemType type,String name){
        var system = SolarSystem.builder()
                .name(name)
                .viewName(name.toUpperCase())
                .type(type)
                .creationDate(LocalDateTime.now())
                .influxTagName(name)
                .token(passwordEncoder.encode("token"))
                .ownedBy(user)
                .publicMode(PublicMode.NONE)
                .timezone("UTC")
                .build();

        return solarSystemRepository.save(system);
    }

    protected String signIn() {
        return signIn("test");
    }

    protected String signIn(String username) {

        var dto = UserLoginDTO.builder()
                .name(username)
                .password("password")
                .build();

        var ret = doRestRequest("/api/user/login", dto);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode nameNode = null;
        try {
            nameNode = mapper.readTree(ret.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return nameNode.get("jwt").asText();
    }

    public void pushDataSample(String systemId, SampleDTO dto) throws JsonProcessingException {
        doRestRequest("api/solar/data?systemId="+systemId, objectMapper.writeValueAsString(dto), HttpMethod.POST, Collections.singletonMap("clientToken", "token"));
    }
}
