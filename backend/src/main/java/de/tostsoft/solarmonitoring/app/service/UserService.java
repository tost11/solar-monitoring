package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.JwtUtil;
import de.tostsoft.solarmonitoring.app.dtos.GenericDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.EditUserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.lib.model.RegisterUser;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.repository.*;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegisterUserRepository registerUserRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtUtil jwtTokenUnit;

    @Autowired
    private InfluxConnection influxConnection;

    @Value("${user.defaultNumSystems}")
    private int defaultNumSystems;

    @Value("${monitoring.mail:#{null}}")
    private String monitoringMail;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private MailService mailService;

    @Value("${fulldomain}")
    private String fulldomain;

    @Autowired
    private ConfigService configService;
    @Autowired
    private ManagesRepository managesRepository;
    
    @Autowired
    private JWTSessionTokenRepository jwtTokenRepository;

    public UserDTO loginUser(UserLoginDTO userLoginDTO) {
        var authentication = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
        var user = (User) authentication.getPrincipal();

        String jwt = jwtTokenUnit.generateJWT(user);
        UserDTO userDTO = new UserDTO(user.getId(), user.getViewName());
        userDTO.setJwt(jwt);
        userDTO.setAdmin(user.getIsAdmin());
        return userDTO;
    }

    public RegisterUser registerUser(UserRegisterDTO userRegisterDTO) {

        var id = new ObjectId();

        RegisterUser user = RegisterUser.builder()
                .id(id.toString())
                .name(StringUtils.lowerCase(userRegisterDTO.getName()))
                .viewName(userRegisterDTO.getName())
                .mail(userRegisterDTO.getMail())
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .createdAt(Instant.now().toEpochMilli())
                .build();

        LOG.info("Created new user with name: {}", user.getName());

        user = registerUserRepository.save(user);

        configService.increaseDailyRegistrations();

        mailService.sendMail(user.getMail(),"Solar Monitoring Activation","Hallo, "+user.getViewName()+" the registration is done!\nActivate your account here: "+fulldomain+"/api/user/activate/"+user.getId());

        try{
            if(monitoringMail != null) {
                mailService.sendMail(monitoringMail, "Solar-Monitoring new user registration", "A new user with name: "+user.getName()+" has been registered!");
            }
        }catch (Exception ex){
            LOG.warn("Could not send monitoring mail on user registration: {}",ex.getMessage());
        }

        return user;
        //now wait for clicking on registrationlink
    }

    public boolean checkUsernameAlreadyTaken(UserRegisterDTO userRegisterDTO) {
        return userRepository.countByNameWithDeleted(StringUtils.lowerCase(userRegisterDTO.getName())) != 0 || registerUserRepository.countByName(StringUtils.lowerCase(userRegisterDTO.getName())) != 0;
    }

    public boolean checkUserMailAlreadyTaken(UserRegisterDTO userRegisterDTO) {
        return userRepository.countByMailWithDeleted(StringUtils.lowerCase(userRegisterDTO.getMail())) != 0 || registerUserRepository.countByMail(StringUtils.lowerCase(userRegisterDTO.getMail())) != 0;
    }

    UserForAdminDTO convertUserToUserForAdminDTO(User user,boolean isDeleted) {
        return UserForAdminDTO.builder()
                .id(user.getId())
                .isAdmin(user.getIsAdmin())
                .name(user.getName())
                .numAllowedSystems(user.getNumAllowedSystems())
                .creationDate(user.getCreationDate().atZone(ZoneId.of("UTC")))
                .isDeleted(isDeleted)
                .build();
    }

    public UserForAdminDTO editUser(EditUserForAdminDTO userDTO) {
        var userOpt = userRepository.findByIdWithDeleted(userDTO.getId());
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var user = userOpt.get();
        user.setName(userDTO.getName());
        user.setIsAdmin(userDTO.isAdmin());
        user.setNumAllowedSystems(userDTO.getNumAllowedSystems());
        user.setMail(userDTO.getMail());

        if(!userDTO.isDeleted()){
            user.setDeletedAt(null);
        }
        user = userRepository.save(user);
        if(userDTO.isDeleted()){
            deleteUserWithAllSystemsAnRelations(user);
        }
        return convertUserToUserForAdminDTO(user,userDTO.isDeleted());
    }

    public Collection<UserForAdminDTO> findUserForAdmin(String name) {
        var lowerName = StringUtils.lowerCase(name);

        //map needet because maby user is in deleted and not deleted users at the same time (cleanup job will fix that)
        Map<String,UserForAdminDTO> userDTOS = new HashMap<>();

        List<User> userList = userRepository.findAllByNameStartingWithWithDeleted(lowerName);
        for(var user : userList){
            UserForAdminDTO userDTO = UserForAdminDTO.builder()
                    .id(user.getId())
                    .isAdmin(user.getIsAdmin())
                    .name(user.getName())
                    .numAllowedSystems(user.getNumAllowedSystems())
                    .creationDate(user.getCreationDate().atZone(ZoneId.of("UTC")))
                    .isDeleted(user.getDeletedAt() != null)
                    .mail(user.getMail())
                    .build();

            userDTOS.put(user.getId(),userDTO);
        }

        return userDTOS.values();
    }

    public List<GenericDataDTO> findUsers(String name) {
        List<User> userList = userRepository.findAllByNameStartingWith(StringUtils.lowerCase(name));
        return userList.stream().map(u->new GenericDataDTO(u.getId(),u.getName())).collect(Collectors.toList());
    }

    public boolean isUserFromContextAdmin(){
        var auth =SecurityContextHolder.getContext().getAuthentication();
        if(auth == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        var user = (User) auth.getPrincipal();
        if(user == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        return userRepository.countByIdAndIsAdminWithDeleted(user.getId(),true) > 0;
    }

    public User getLoggedInUserFull(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        var user = (User) auth.getPrincipal();
        if(user == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        var userOpt = userRepository.findById(user.getId());
        if(userOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return userOpt.get();
    }

    public void deleteUserWithAllSystemsAnRelations(User user){
        var deleteAtt = LocalDateTime.now(ZoneOffset.UTC);
        userRepository.setDeleteAt(user.getId(),deleteAtt);
        for (SolarSystem system : user.getOwns()) {
            managesRepository.setDeleteAtOnAllRelationBySolarSystem(system.getId(),deleteAtt);
        }
        solarSystemRepository.setDeleteAtOnAllActiveSystemsByOwner(user.getId(),deleteAtt);
        managesRepository.setDeleteAtOnAllRelationByUser(user.getId(),deleteAtt);
    }

    public User getLoggedInUserFullNoException(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null){
            return null;
        }
        var user = (User) auth.getPrincipal();
        if(user == null){
            return null;
        }
        var userOpt = userRepository.findById(user.getId());
        return userOpt.orElse(null);
    }

    public User activateUser(String userId){

        var registerUser = registerUserRepository.findById(userId).orElse(null);
        if(registerUser == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found");
        }

        var id = new ObjectId();

         var user = User.builder()
            .id(id.toString())
            .name(registerUser.getName())
            .viewName(registerUser.getViewName())
            .mail(registerUser.getMail())
            .creationDate(LocalDateTime.now())
            .influxBucketName(id.toString())
            .numAllowedSystems(defaultNumSystems)
            .password(registerUser.getPassword())
            .isAdmin(false)
            .activated(false)
            .build();

        user = userRepository.save(user);

        influxConnection.createNewBucket(user.getId());
        user.setInfluxBucketName(user.getId());

        LOG.info("New user activated");

        try{
            if(monitoringMail != null) {
                mailService.sendMail(monitoringMail, "Solar-Monitoring user activated", "the user user with name: "+user.getName()+" has been activated!");
            }
        }catch (Exception ex){
            LOG.warn("Could not send monitoring mail on user activation: {}",ex.getMessage());
        }

        return user;
    }

    public void signOutCurrentContext(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        
        String tokenId = auth.getCredentials().toString();
        jwtTokenRepository.deleteById(tokenId);
    }
}
