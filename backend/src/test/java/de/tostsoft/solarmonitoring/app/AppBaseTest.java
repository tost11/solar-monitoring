package de.tostsoft.solarmonitoring.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.influxdb.client.domain.Bucket;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.lib.service.AesGcmService;
import de.tostsoft.solarmonitoring.testlib.BaseRestTest;
import de.tostsoft.solarmonitoring.testlib.service.MailhogTestService;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@SpringBootTest(classes = {SolarmonitoringApplication.class},webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppBaseTest extends BaseRestTest {

    @Autowired
    protected InfluxConnection influxConnection;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RegisterUserRepository registerUserRepository;

    @Autowired
    protected CaptchaRepository captchaRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected SolarSystemRepository solarSystemRepository;

    @Autowired
    protected ManagesRepository managesRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected ConfigRepository configRepository;

    @Autowired
    protected JWTSessionTokenRepository jwtSessionTokenRepository;

    @Autowired
    protected PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected LoginAttemptRepository loginAttemptRepository;

    @Autowired
    protected AccountLockoutRepository accountLockoutRepository;

    //protected ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    protected MailhogTestService mailhogTestService;

    @LocalServerPort
    private int randomServerPort;

    @Override
    protected int getServerPort() {
        return randomServerPort;
    }

    @Value("${configNode:root}")
    private String configName;

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
        registerUserRepository.deleteAll();
        captchaRepository.deleteAll();
        configRepository.deleteAll();
        jwtSessionTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        accountLockoutRepository.deleteAll();

        mailhogTestService.deleteAllMessages();

        //init
        configRepository.save(Config.builder()
            .dailyRegistrations(0)
            .isRegistrationEnabled(true)
            .name(configName)
            .build());
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected User addUser(boolean admin){
        return addUser(admin,"test");
    }

    protected User addUser(boolean admin,String name){
        var user = User.builder()
            .name(StringUtils.toRootLowerCase(name))
            .viewName(name)
            .password(passwordEncoder.encode("password"))
            .isAdmin(admin)
            .mail(StringUtils.toRootLowerCase(name)+"@local.host")
            .creationDate(LocalDateTime.now(ZoneOffset.UTC))
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
            .systemInformations(SystemInformations.builder()
                    .viewName(name)
                    .name(name).build())
            .type(type)
            .creationDate(LocalDateTime.now())
            .influxTagName(name)
            .tokens(List.of(
                AccessToken.builder()
                    .id(new ObjectId().toString())
                    .name("default-rest")
                    .hash(passwordEncoder.encode("token"))
                    .purpose(TokenPurpose.DATA_PUSH_REST)
                    .createdAt(LocalDateTime.now())
                    .build(),
                AccessToken.builder()
                    .id(new ObjectId().toString())
                    .name("default-aes-gcm")
                    .hash(AesGcmService.sha256Hex("token"))
                    .purpose(TokenPurpose.DATA_PUSH_ENCRYPTED)
                    .createdAt(LocalDateTime.now())
                    .build()
            ))
            .ownedBy(user)
            .publicMode(PublicMode.NONE)
            .timezone("UTC")
            .totalValues(TotalValues.builder().build())
            .viewData(ViewData.builder().build())
            .build();

        return solarSystemRepository.save(system);
    }

    protected Manages addManges(SolarSystem solarSystem,User user){
        var manages = Manages.builder()
            .solarSystem(solarSystem)
            .user(user)
            .permission(Permissions.ADMIN)
            .build();

        return managesRepository.save(manages);
    }

    protected String signIn() {
        return signIn("test");
    }

    protected String signIn(String username) {
        return signIn(username,"password");
    }

    protected String signIn(String username,String password) {
        var dto = UserLoginDTO.builder()
            .name(username)
            .password(password)
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

    protected Tag addTag(String name, String color) {
        return addTag(name, color, false);
    }

    protected Tag addTag(String name, String color, boolean locked) {
        return addTag(name, color, locked, false, false);
    }

    protected Tag addTag(String name, String color, boolean locked, boolean showOnStartPage, boolean showStartPageAggregation) {
        Tag tag = Tag.builder()
            .name(name)
            .viewName(name)  // viewName is required (@NonNull)
            .color(color)
            .locked(locked)  // locked is required (@NonNull)
            .showOnStartPage(showOnStartPage)
            .showStartPageAggregation(showStartPageAggregation)
            .build();
        return tagRepository.save(tag);
    }
}
